// ---------------------------------------------------------------------------
// FPPDFFramework_jni.cpp : JNI bridge between the Kotlin UI layer
// (FPPDFNative) and the C++ FPPDFFramework public API
// (FPPDFFramework.h / FPPDFOptions.h).
//
// This is the minimal C++ integration path: the app writes its own JNI bridge
// on top of the C++ API.  For the zero-native-code path, see the sibling
// FPPDFFramework_Demo_Android project (pure Kotlin API).
//
// Copyright (c) 2026 Flyingbee Software. All rights reserved.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>

#include <cstdio>
#include <cstring>
#include <pthread.h>

#include <FPPDFFramework.h>
#include <FPPDFOptions.h>

#define LOG_TAG "FPPDFJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;

namespace {

// ---------------------------------------------------------------------------
// JNI delegate: forwards the C++ FPPDF2AllConverterDelegate callbacks into the
// Kotlin listener object (com.flyingbee.FPPDFConverterDemoCpp.DelegateBridge
// or any object implementing the same method signatures).
// ---------------------------------------------------------------------------
class JniConverterDelegate : public FPPDF2AllConverterDelegate
{
public:
    JniConverterDelegate(JNIEnv* env, jobject listener)
    {
        obj = env->NewGlobalRef(listener);
        jclass cls = env->GetObjectClass(listener);
        midDidStart = env->GetMethodID(cls, "didStartConversion", "(ZLjava/lang/String;)V");
        midDidEnd = env->GetMethodID(cls, "didEndConversion", "(ZLjava/lang/String;)V");
        midDidEndPage = env->GetMethodID(cls, "didEndPageIndex", "(IIZLjava/lang/String;)V");
        midWillSave = env->GetMethodID(cls, "willSaveDoc", "()V");
        midCatchException = env->GetMethodID(cls, "catchException", "()V");
    }

    // The SDK delegate base class has no virtual destructor; this object is
    // only ever deleted through the concrete JniConverterDelegate* held in
    // ConverterHandle, so a plain destructor is safe here.
    ~JniConverterDelegate()
    {
        if (obj) {
            // Called from the main thread (nativeDestroyConverter); the SDK
            // worker threads have already been joined by the converter's
            // destructor, so no callback can race with this release.
            AttachGuard g;
            if (g.env) {
                g.env->DeleteGlobalRef(obj);
                obj = nullptr;
            }
        }
    }

    void FPPDF2AllConverterDelegate_didStartConversion(FPPDF2AllConverter* /*converterA*/, bool result, char* errorInfo) override
    {
        AttachGuard g;
        if (!g.env) return;
        jstring msg = toJavaString(g.env, errorInfo);
        g.env->CallVoidMethod(obj, midDidStart, result ? JNI_TRUE : JNI_FALSE, msg);
        g.env->DeleteLocalRef(msg);
    }

    void FPPDF2AllConverterDelegate_didEndConversion(FPPDF2AllConverter* /*converterA*/, bool result, char* errorInfo) override
    {
        AttachGuard g;
        if (!g.env) return;
        jstring msg = toJavaString(g.env, errorInfo);
        g.env->CallVoidMethod(obj, midDidEnd, result ? JNI_TRUE : JNI_FALSE, msg);
        g.env->DeleteLocalRef(msg);
    }

    void FPPDF2AllConverterDelegate_didEndPageIndex(FPPDF2AllConverter* /*converterA*/, int toPageIndexA, int toTotalPagesA, bool result, char* errorInfo) override
    {
        AttachGuard g;
        if (!g.env) return;
        jstring msg = toJavaString(g.env, errorInfo);
        g.env->CallVoidMethod(obj, midDidEndPage, toPageIndexA, toTotalPagesA, result ? JNI_TRUE : JNI_FALSE, msg);
        g.env->DeleteLocalRef(msg);
    }

    void FPPDF2AllConverterDelegate_willSaveDoc(FPPDF2AllConverter* /*converterA*/) override
    {
        AttachGuard g;
        if (!g.env) return;
        g.env->CallVoidMethod(obj, midWillSave);
    }

    void FPPDF2AllConverterDelegate_catchException(FPPDF2AllConverter* /*converterA*/, void* /*exception*/) override
    {
        AttachGuard g;
        if (!g.env) return;
        g.env->CallVoidMethod(obj, midCatchException);
    }

private:
    // Attaches the current thread to ART for the duration of one callback and
    // detaches it again on scope exit (unless the thread was already attached,
    // e.g. a Kotlin thread or the main thread). This guarantees that SDK
    // worker pthreads are never left attached when they exit - on ART < 11 an
    // attached-but-undetached native thread aborts the whole process
    // ("Native thread exited without calling DetachCurrentThread").
    struct AttachGuard
    {
        JNIEnv* env = nullptr;
        bool attachedHere = false;

        AttachGuard()
        {
            if (!g_jvm) return;
            jint rc = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
            if (rc == JNI_EDETACHED) {
                if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
                    attachedHere = true;
                } else {
                    env = nullptr;
                }
            } else if (rc != JNI_OK) {
                env = nullptr;
            }
        }

        ~AttachGuard()
        {
            if (attachedHere && g_jvm) {
                g_jvm->DetachCurrentThread();
            }
        }

        AttachGuard(const AttachGuard&) = delete;
        AttachGuard& operator=(const AttachGuard&) = delete;
    };

    static jstring toJavaString(JNIEnv* env, const char* utf8)
    {
        if (!utf8) return env->NewStringUTF("");
        return env->NewStringUTF(utf8);
    }

    jobject obj = nullptr;
    jmethodID midDidStart = nullptr;
    jmethodID midDidEnd = nullptr;
    jmethodID midDidEndPage = nullptr;
    jmethodID midWillSave = nullptr;
    jmethodID midCatchException = nullptr;
};

// ---------------------------------------------------------------------------
// Options holder: allocated by nativeNewOptions, filled by the per-group
// setters, consumed (deep-copied) by convertPDFItem and freed by
// nativeFreeOptions.  Field order mirrors FPPDFOptions.h.
// ---------------------------------------------------------------------------
struct OptionsBag
{
    // General
    int isParserAnnots = 1;
    int threadMax = 2;
    int imageDPI = 144;
    float imageQuality = 0.92f;
    bool isEnableOCR = false;

    // Word / HTML
    bool wordTrimBlankSpace = true;
    bool wordMergeParagraphs = false;
    int wordOutlineType = 1;
    bool wordShapeToImage = true;
    bool wordMergeIntersectImages = true;
    int htmlLayoutMode = 0;
    int htmlMergeResource = 2;
    int htmlNavigationBar = 1;
    int htmlTextFlowParagraph = 0;
    int htmlPackage = 0;

    // Excel
    int excelFormatOption = 0;
    int excelThousandSeparator = 0;
    bool excelAllInOneSheet = false;
    bool excelAllInOneSheetAddToRow = true;
    bool excelRecognizeNumber = true;
    int excelOverlapText = 0;
    bool excelCsvPackageZip = false;

    // Image
    int imageFormat = 1; // PNG
    int imageOutputDPI = 144;
    float imageOutputQuality = 0.92f;
    bool imagePackageZip = false;
    bool imageAntiAlias = true;

    // Element
    float elementQuality = 0.92f;
    bool elementPackageZip = false;

    // OCR
    char ocrLanguage[128] = "eng";
    int ocrEngineMode = 1; // LSTM only
    unsigned int ocrResizeDPI = 300;
    float ocrMinConfidence = 10.0f;
    bool ocrImageScan = false;

    FPPDFOptions* build() const
    {
        FPPDFOptions* o = new FPPDFOptions();
        o->isParserAnnots = isParserAnnots;
        o->threadMax = threadMax;
        o->imageDPI = imageDPI;
        o->imageQuality = imageQuality;
        o->isEnableOCR = isEnableOCR;

        o->wordOptions->isTrimmingBlankSpaceCharacters = wordTrimBlankSpace;
        o->wordOptions->isMergeParagraphs = wordMergeParagraphs;
        o->wordOptions->outlineType = (FPWordOption_Outline_Type)wordOutlineType;
        o->wordOptions->enableShapeToImage = wordShapeToImage;
        o->wordOptions->enableMergeIntersectImages = wordMergeIntersectImages;
        o->wordOptions->htmlLayoutMode = (FPWordOption_HTML_LayoutMode)htmlLayoutMode;
        o->wordOptions->htmlMergeResource = (FPWordOption_HTML_Merge_Resource)htmlMergeResource;
        o->wordOptions->htmlNavigationBar = (FPWordOption_HTML_NavigationBar)htmlNavigationBar;
        o->wordOptions->htmlTextFlowParagraph = (FPWordOption_HTML_TextFlow_Paragraph)htmlTextFlowParagraph;
        o->wordOptions->htmlPackage = (FPWordOption_HTML_Package)htmlPackage;

        o->excelOptions->excelFormatOption = (FPPDFToExcelFormatOption)excelFormatOption;
        o->excelOptions->thousandSeparator = (FPPDFToExcelthousandSeparator)excelThousandSeparator;
        o->excelOptions->allInOneSheet = excelAllInOneSheet;
        o->excelOptions->allInOneSheetAddToRow = excelAllInOneSheetAddToRow;
        o->excelOptions->recognizeNumber = excelRecognizeNumber;
        o->excelOptions->overlapText = (FPPDFToExcelOverlapText)excelOverlapText;
        o->excelOptions->isCSVPackageZip = excelCsvPackageZip ? 1 : 0;

        o->imageOptions->imageFormat = (FPPDF2ImageOptions_Format)imageFormat;
        o->imageOptions->imageDPI = imageOutputDPI;
        o->imageOptions->imageQuality = imageOutputQuality;
        o->imageOptions->isPackageZip = imagePackageZip ? 1 : 0;
        o->imageOptions->isAntiAlias = imageAntiAlias;

        o->elementOptions->imageQuality = elementQuality;
        o->elementOptions->isPackageZip = elementPackageZip ? 1 : 0;

        snprintf(o->ocrOptions->language, sizeof(o->ocrOptions->language), "%s", ocrLanguage);
        o->ocrOptions->engineMode = (FPPDFOCREngineMode)ocrEngineMode;
        o->ocrOptions->resizeDPI = ocrResizeDPI;
        o->ocrOptions->minConfidence = ocrMinConfidence;
        o->ocrOptions->isEnableImageScan = ocrImageScan;

        return o;
    }
};

// ---------------------------------------------------------------------------
// Converter handle
// ---------------------------------------------------------------------------
struct ConverterHandle
{
    FPPDF2AllConverter* converter = nullptr;
    JniConverterDelegate* delegate = nullptr;
};

} // namespace

// ===========================================================================
// JNI entry points
// ===========================================================================

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/)
{
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// --- SDK identity / license -------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSdkVersion(JNIEnv* env, jclass)
{
    return env->NewStringUTF(FPPDFFramework_Version);
}

JNIEXPORT jstring JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeReleaseDate(JNIEnv* env, jclass)
{
    return env->NewStringUTF(FPPDFFramework_ReleaseDate ? FPPDFFramework_ReleaseDate : "");
}

JNIEXPORT jstring JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeLicenseOrganization(JNIEnv* env, jclass)
{
    const char* org = FPPDF2AllConverter::GetSDKLicenseOrganization();
    return env->NewStringUTF(org ? org : "");
}

JNIEXPORT jstring JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeLicenseExpiredDate(JNIEnv* env, jclass)
{
    const char* date = FPPDF2AllConverter::GetSDKLicenseExpiredDate();
    return env->NewStringUTF(date ? date : "");
}

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeLicenseIsExpired(JNIEnv*, jclass)
{
    return FPPDF2AllConverter::isSDKLicenseAuth_ExpiredDate() ? JNI_TRUE : JNI_FALSE;
}

// --- Resource root (Android: Resources.bundle lives in app-private storage) --

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetResourceRootFolder(JNIEnv* env, jclass, jstring path)
{
    const char* utf = env->GetStringUTFChars(path, nullptr);
    FPPDF2AllConverter::SetResourceRootFolder(utf);
    env->ReleaseStringUTFChars(path, utf);
}

// --- PDF document inspection (FPPDFDocument) --------------------------------

JNIEXPORT jlong JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeOpenDocument(JNIEnv* env, jclass,
                                                                     jstring path, jstring ownerPwd, jstring userPwd)
{
    const char* p = env->GetStringUTFChars(path, nullptr);
    const char* ow = ownerPwd ? env->GetStringUTFChars(ownerPwd, nullptr) : nullptr;
    const char* up = userPwd ? env->GetStringUTFChars(userPwd, nullptr) : nullptr;
    FPPDFDocument* doc = new FPPDFDocument(p, ow ? ow : "", up ? up : "");
    if (ow) env->ReleaseStringUTFChars(ownerPwd, ow);
    if (up) env->ReleaseStringUTFChars(userPwd, up);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(doc);
}

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeDocumentIsOpen(JNIEnv*, jclass, jlong handle)
{
    auto* doc = reinterpret_cast<FPPDFDocument*>(handle);
    return doc && doc->isOpenSuccess() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeDocumentIsEncrypted(JNIEnv*, jclass, jlong handle)
{
    auto* doc = reinterpret_cast<FPPDFDocument*>(handle);
    return doc && doc->isEncrypted() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeDocumentHasPassword(JNIEnv*, jclass, jlong handle)
{
    auto* doc = reinterpret_cast<FPPDFDocument*>(handle);
    return doc && doc->isHavePassword() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeDocumentPageCount(JNIEnv*, jclass, jlong handle)
{
    auto* doc = reinterpret_cast<FPPDFDocument*>(handle);
    return doc ? doc->pageCount() : 0;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeCloseDocument(JNIEnv*, jclass, jlong handle)
{
    auto* doc = reinterpret_cast<FPPDFDocument*>(handle);
    // ~FPPDFDocument already deletes the internal document through its pimpl;
    // calling deletePDFDocument() here as well would double-free it (the SDK
    // does not null the inner pointer), which aborts under Scudo.
    delete doc;
}

// --- Converter lifecycle -----------------------------------------------------

JNIEXPORT jlong JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeCreateConverter(JNIEnv* env, jclass, jobject listener)
{
    auto* h = new ConverterHandle();
    h->delegate = new JniConverterDelegate(env, listener);
    h->converter = new FPPDF2AllConverter(h->delegate);
    return reinterpret_cast<jlong>(h);
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeDestroyConverter(JNIEnv*, jclass, jlong handle)
{
    auto* h = reinterpret_cast<ConverterHandle*>(handle);
    if (!h) return;
    delete h->converter;
    delete h->delegate;
    delete h;
}

// --- Options -----------------------------------------------------------------

JNIEXPORT jlong JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeNewOptions(JNIEnv*, jclass)
{
    return reinterpret_cast<jlong>(new OptionsBag());
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeFreeOptions(JNIEnv*, jclass, jlong handle)
{
    delete reinterpret_cast<OptionsBag*>(handle);
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsGeneral(JNIEnv*, jclass, jlong handle,
                                                                          jint isParserAnnots, jint threadMax,
                                                                          jint imageDPI, jfloat imageQuality,
                                                                          jboolean isEnableOCR)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->isParserAnnots = isParserAnnots;
    o->threadMax = threadMax;
    o->imageDPI = imageDPI;
    o->imageQuality = imageQuality;
    o->isEnableOCR = isEnableOCR;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsWord(JNIEnv*, jclass, jlong handle,
                                                                       jboolean trimBlank, jboolean mergeParagraphs,
                                                                       jint outlineType, jboolean shapeToImage,
                                                                       jboolean mergeIntersectImages)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->wordTrimBlankSpace = trimBlank;
    o->wordMergeParagraphs = mergeParagraphs;
    o->wordOutlineType = outlineType;
    o->wordShapeToImage = shapeToImage;
    o->wordMergeIntersectImages = mergeIntersectImages;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsHtml(JNIEnv*, jclass, jlong handle,
                                                                       jint layoutMode, jint mergeResource,
                                                                       jint navigationBar, jint textFlowParagraph,
                                                                       jint packageZip)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->htmlLayoutMode = layoutMode;
    o->htmlMergeResource = mergeResource;
    o->htmlNavigationBar = navigationBar;
    o->htmlTextFlowParagraph = textFlowParagraph;
    o->htmlPackage = packageZip;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsExcel(JNIEnv*, jclass, jlong handle,
                                                                        jint formatOption, jint thousandSeparator,
                                                                        jboolean allInOneSheet, jboolean allInOneAddToRow,
                                                                        jboolean recognizeNumber, jint overlapText,
                                                                        jboolean csvPackageZip)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->excelFormatOption = formatOption;
    o->excelThousandSeparator = thousandSeparator;
    o->excelAllInOneSheet = allInOneSheet;
    o->excelAllInOneSheetAddToRow = allInOneAddToRow;
    o->excelRecognizeNumber = recognizeNumber;
    o->excelOverlapText = overlapText;
    o->excelCsvPackageZip = csvPackageZip;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsImage(JNIEnv*, jclass, jlong handle,
                                                                        jint imageFormat, jint dpi,
                                                                        jfloat quality, jboolean packageZip,
                                                                        jboolean antiAlias)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->imageFormat = imageFormat;
    o->imageOutputDPI = dpi;
    o->imageOutputQuality = quality;
    o->imagePackageZip = packageZip;
    o->imageAntiAlias = antiAlias;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsElement(JNIEnv*, jclass, jlong handle,
                                                                          jfloat quality, jboolean packageZip)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    o->elementQuality = quality;
    o->elementPackageZip = packageZip;
}

JNIEXPORT void JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeSetOptionsOcr(JNIEnv* env, jclass, jlong handle,
                                                                      jstring language, jint engineMode,
                                                                      jint resizeDPI, jfloat minConfidence,
                                                                      jboolean imageScan)
{
    auto* o = reinterpret_cast<OptionsBag*>(handle);
    if (!o) return;
    if (language) {
        const char* s = env->GetStringUTFChars(language, nullptr);
        snprintf(o->ocrLanguage, sizeof(o->ocrLanguage), "%s", s ? s : "eng");
        env->ReleaseStringUTFChars(language, s);
    }
    o->ocrEngineMode = engineMode;
    o->ocrResizeDPI = static_cast<unsigned int>(resizeDPI);
    o->ocrMinConfidence = minConfidence;
    o->ocrImageScan = imageScan;
}

// --- Conversion ---------------------------------------------------------------

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeConvertPdf(JNIEnv* env, jclass, jlong handle,
                                                                   jstring pdfPath, jstring password,
                                                                   jintArray pageIndexes, jstring outputFormat,
                                                                   jstring destPath, jlong optionsHandle,
                                                                   jboolean isInBackground)
{
    auto* h = reinterpret_cast<ConverterHandle*>(handle);
    if (!h || !h->converter) return JNI_FALSE;

    const char* pdf = env->GetStringUTFChars(pdfPath, nullptr);
    const char* pwd = password ? env->GetStringUTFChars(password, nullptr) : nullptr;
    const char* fmt = env->GetStringUTFChars(outputFormat, nullptr);
    const char* dest = env->GetStringUTFChars(destPath, nullptr);

    int* pages = nullptr;
    int pageCount = 0;
    if (pageIndexes) {
        pageCount = env->GetArrayLength(pageIndexes);
        if (pageCount > 0) pages = env->GetIntArrayElements(pageIndexes, nullptr);
    }

    auto* bag = reinterpret_cast<OptionsBag*>(optionsHandle);
    FPPDFOptions* cppOptions = bag ? bag->build() : nullptr;

    bool ok = h->converter->convertPDFItem(pdf, pwd ? pwd : "", pages, pageCount, fmt, dest, cppOptions, isInBackground);

    if (cppOptions) delete cppOptions;
    if (pages) env->ReleaseIntArrayElements(pageIndexes, pages, JNI_ABORT);
    env->ReleaseStringUTFChars(destPath, dest);
    env->ReleaseStringUTFChars(outputFormat, fmt);
    if (pwd) env->ReleaseStringUTFChars(password, pwd);
    env->ReleaseStringUTFChars(pdfPath, pdf);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_flyingbee_FPPDFConverterDemoCpp_FPPDFNative_nativeCancelConversion(JNIEnv*, jclass, jlong handle)
{
    auto* h = reinterpret_cast<ConverterHandle*>(handle);
    if (!h || !h->converter) return JNI_FALSE;
    return h->converter->cancelConversion() ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
