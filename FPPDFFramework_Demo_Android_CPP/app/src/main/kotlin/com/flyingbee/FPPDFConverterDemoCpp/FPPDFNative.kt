package com.flyingbee.FPPDFConverterDemoCpp

// ---------------------------------------------------------------------------
// FPPDFNative : the Kotlin view of the JNI bridge (app/src/main/cpp/
// FPPDFFramework_jni.cpp).  Every function here maps 1:1 onto a public C++
// symbol in libFPPDFFramework.so (FPPDFDocument / FPPDF2AllConverter /
// FPPDFOptions).
// ---------------------------------------------------------------------------

/**
 * Callbacks forwarded from the C++ FPPDF2AllConverterDelegate. All methods
 * may be called on a background (SDK worker) thread; the controller hops to
 * the main thread before touching UI state.
 */
interface FPPDFDelegate {
    fun didStartConversion(result: Boolean, errorInfo: String?)
    fun didEndConversion(result: Boolean, errorInfo: String?)
    fun didEndPageIndex(toPageIndex: Int, toTotalPages: Int, result: Boolean, errorInfo: String?)
    fun willSaveDoc()
    fun catchException()
}

object FPPDFNative {

    init {
        // libFPPDFFramework.so is loaded transitively as a dependency of
        // libFPPDFFramework_jni.so (both are packaged from the SDK AAR /
        // this module).
        System.loadLibrary("FPPDFFramework_jni")
    }

    // The external declarations carry the exact `nativeXxx` names exported by
    // FPPDFFramework_jni.cpp (Java_com_..._FPPDFNative_nativeXxx); the public
    // wrappers below keep the call sites readable.

    // --- SDK identity / license ---------------------------------------------
    private external fun nativeSdkVersion(): String
    private external fun nativeReleaseDate(): String
    private external fun nativeLicenseOrganization(): String
    private external fun nativeLicenseExpiredDate(): String
    private external fun nativeLicenseIsExpired(): Boolean

    fun sdkVersion(): String = nativeSdkVersion()
    fun releaseDate(): String = nativeReleaseDate()
    fun licenseOrganization(): String = nativeLicenseOrganization()
    fun licenseExpiredDate(): String = nativeLicenseExpiredDate()
    fun licenseIsExpired(): Boolean = nativeLicenseIsExpired()

    // --- SDK resource root ------------------------------------------------------
    private external fun nativeSetResourceRootFolder(path: String)
    fun setResourceRootFolder(path: String) = nativeSetResourceRootFolder(path)

    // --- PDF document inspection ---------------------------------------------
    private external fun nativeOpenDocument(path: String, ownerPassword: String?, userPassword: String?): Long
    private external fun nativeDocumentIsOpen(handle: Long): Boolean
    private external fun nativeDocumentIsEncrypted(handle: Long): Boolean
    private external fun nativeDocumentHasPassword(handle: Long): Boolean
    private external fun nativeDocumentPageCount(handle: Long): Int
    private external fun nativeCloseDocument(handle: Long)

    fun openDocument(path: String, ownerPassword: String?, userPassword: String?): Long =
        nativeOpenDocument(path, ownerPassword, userPassword)
    fun documentIsOpen(handle: Long): Boolean = nativeDocumentIsOpen(handle)
    fun documentIsEncrypted(handle: Long): Boolean = nativeDocumentIsEncrypted(handle)
    fun documentHasPassword(handle: Long): Boolean = nativeDocumentHasPassword(handle)
    fun documentPageCount(handle: Long): Int = nativeDocumentPageCount(handle)
    fun closeDocument(handle: Long) = nativeCloseDocument(handle)

    // --- Converter lifecycle ---------------------------------------------------
    private external fun nativeCreateConverter(listener: DelegateBridge): Long
    private external fun nativeDestroyConverter(handle: Long)

    fun createConverter(listener: DelegateBridge): Long = nativeCreateConverter(listener)
    fun destroyConverter(handle: Long) = nativeDestroyConverter(handle)

    // --- Options -----------------------------------------------------------------
    private external fun nativeNewOptions(): Long
    private external fun nativeFreeOptions(handle: Long)
    private external fun nativeSetOptionsGeneral(handle: Long, isParserAnnots: Int, threadMax: Int, imageDPI: Int, imageQuality: Float, isEnableOCR: Boolean)
    private external fun nativeSetOptionsWord(handle: Long, trimBlank: Boolean, mergeParagraphs: Boolean, outlineType: Int, shapeToImage: Boolean, mergeIntersectImages: Boolean)
    private external fun nativeSetOptionsHtml(handle: Long, layoutMode: Int, mergeResource: Int, navigationBar: Int, textFlowParagraph: Int, packageZip: Int)
    private external fun nativeSetOptionsExcel(handle: Long, formatOption: Int, thousandSeparator: Int, allInOneSheet: Boolean, allInOneAddToRow: Boolean, recognizeNumber: Boolean, overlapText: Int, csvPackageZip: Boolean)
    private external fun nativeSetOptionsImage(handle: Long, imageFormat: Int, dpi: Int, quality: Float, packageZip: Boolean, antiAlias: Boolean)
    private external fun nativeSetOptionsElement(handle: Long, quality: Float, packageZip: Boolean)
    private external fun nativeSetOptionsOcr(handle: Long, language: String, engineMode: Int, resizeDPI: Int, minConfidence: Float, imageScan: Boolean)

    fun newOptions(): Long = nativeNewOptions()
    fun freeOptions(handle: Long) = nativeFreeOptions(handle)
    fun setOptionsGeneral(handle: Long, isParserAnnots: Int, threadMax: Int, imageDPI: Int, imageQuality: Float, isEnableOCR: Boolean) =
        nativeSetOptionsGeneral(handle, isParserAnnots, threadMax, imageDPI, imageQuality, isEnableOCR)
    fun setOptionsWord(handle: Long, trimBlank: Boolean, mergeParagraphs: Boolean, outlineType: Int, shapeToImage: Boolean, mergeIntersectImages: Boolean) =
        nativeSetOptionsWord(handle, trimBlank, mergeParagraphs, outlineType, shapeToImage, mergeIntersectImages)
    fun setOptionsHtml(handle: Long, layoutMode: Int, mergeResource: Int, navigationBar: Int, textFlowParagraph: Int, packageZip: Int) =
        nativeSetOptionsHtml(handle, layoutMode, mergeResource, navigationBar, textFlowParagraph, packageZip)
    fun setOptionsExcel(handle: Long, formatOption: Int, thousandSeparator: Int, allInOneSheet: Boolean, allInOneAddToRow: Boolean, recognizeNumber: Boolean, overlapText: Int, csvPackageZip: Boolean) =
        nativeSetOptionsExcel(handle, formatOption, thousandSeparator, allInOneSheet, allInOneAddToRow, recognizeNumber, overlapText, csvPackageZip)
    fun setOptionsImage(handle: Long, imageFormat: Int, dpi: Int, quality: Float, packageZip: Boolean, antiAlias: Boolean) =
        nativeSetOptionsImage(handle, imageFormat, dpi, quality, packageZip, antiAlias)
    fun setOptionsElement(handle: Long, quality: Float, packageZip: Boolean) =
        nativeSetOptionsElement(handle, quality, packageZip)
    fun setOptionsOcr(handle: Long, language: String, engineMode: Int, resizeDPI: Int, minConfidence: Float, imageScan: Boolean) =
        nativeSetOptionsOcr(handle, language, engineMode, resizeDPI, minConfidence, imageScan)

    // --- Conversion ------------------------------------------------------------------
    private external fun nativeConvertPdf(
        handle: Long,
        pdfPath: String,
        password: String?,
        pageIndexes: IntArray?,
        outputFormat: String,
        destPath: String,
        optionsHandle: Long,
        isInBackground: Boolean
    ): Boolean

    private external fun nativeCancelConversion(handle: Long): Boolean

    fun convertPdf(
        handle: Long,
        pdfPath: String,
        password: String?,
        pageIndexes: IntArray?,
        outputFormat: String,
        destPath: String,
        optionsHandle: Long,
        isInBackground: Boolean
    ): Boolean = nativeConvertPdf(handle, pdfPath, password, pageIndexes, outputFormat, destPath, optionsHandle, isInBackground)

    fun cancelConversion(handle: Long): Boolean = nativeCancelConversion(handle)
}

/**
 * JNI-visible shim: the C++ side calls these exact method names/signatures
 * (see FPPDFFramework_jni.cpp). It forwards to a mutable [FPPDFDelegate] so the
 * controller can be swapped without recreating the native converter.
 */
@Suppress("unused")
class DelegateBridge(private var delegate: FPPDFDelegate?) {

    fun setDelegate(d: FPPDFDelegate?) { delegate = d }

    // Signatures must match the jmethodIDs cached in JniConverterDelegate.
    fun didStartConversion(result: Boolean, errorInfo: String?) {
        delegate?.didStartConversion(result, errorInfo)
    }

    fun didEndConversion(result: Boolean, errorInfo: String?) {
        delegate?.didEndConversion(result, errorInfo)
    }

    fun didEndPageIndex(toPageIndex: Int, toTotalPages: Int, result: Boolean, errorInfo: String?) {
        delegate?.didEndPageIndex(toPageIndex, toTotalPages, result, errorInfo)
    }

    fun willSaveDoc() {
        delegate?.willSaveDoc()
    }

    fun catchException() {
        delegate?.catchException()
    }
}
