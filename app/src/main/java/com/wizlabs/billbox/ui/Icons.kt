package com.wizlabs.billbox.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Wiz Bill Box icon set (N8): 24 dp grid, 1.75 dp stroke, rounded caps and joins.
 * Replaces material-icons-extended; call sites keep the `Icons.Rounded.X` shape.
 */
object Icons {
    object Rounded {
        val Close: ImageVector by lazy { line("Close", "M6 6l12 12 M18 6L6 18", false) }
        val Check: ImageVector by lazy { line("Check", "M5 12.5l4.5 4.5L19 7.5", false) }
        val Add: ImageVector by lazy { line("Add", "M12 5v14 M5 12h14", false) }
        val ArrowBack: ImageVector by lazy { line("ArrowBack", "M19 12H5 M11 6l-6 6 6 6", true) }
        val Search: ImageVector by lazy { line("Search", "M11 4.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13z M16 16l4.5 4.5", false) }
        val SearchOff: ImageVector by lazy { line("SearchOff", "M11 4.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13z M16 16l4.5 4.5 M9 9l4 4 M13 9l-4 4", false) }
        val CloudDone: ImageVector by lazy { line("CloudDone", "M7 18h10a4 4 0 0 0 .6-7.95A5.5 5.5 0 0 0 7.2 9.1 4.5 4.5 0 0 0 7 18z M9.5 13.5l2 2 3.5-3.5", false) }
        val CloudUpload: ImageVector by lazy { line("CloudUpload", "M7 18h10a4 4 0 0 0 .6-7.95A5.5 5.5 0 0 0 7.2 9.1 4.5 4.5 0 0 0 7 18z M12 16v-5 M9.8 12.8L12 10.6l2.2 2.2", false) }
        val CloudOff: ImageVector by lazy { line("CloudOff", "M7 18h10a4 4 0 0 0 .6-7.95A5.5 5.5 0 0 0 7.2 9.1 4.5 4.5 0 0 0 7 18z M4 4l16 16", false) }
        val CloudSync: ImageVector by lazy { line("CloudSync", "M7 18h10a4 4 0 0 0 .6-7.95A5.5 5.5 0 0 0 7.2 9.1 4.5 4.5 0 0 0 7 18z M14.5 11.5a2.7 2.7 0 0 0-5 1 M9.5 15.5a2.7 2.7 0 0 0 5-1", false) }
        val Verified: ImageVector by lazy { line("Verified", "M12 3l7 3v5.5c0 4.2-2.9 7.7-7 9-4.1-1.3-7-4.8-7-9V6z M9 12l2.2 2.2L15.2 10", false) }
        val ReceiptLong: ImageVector by lazy { line("ReceiptLong", "M6 3h12v18l-2.4-1.6L13.2 21 12 20.2 10.8 21l-2.4-1.6L6 21z M9 8h6 M9 12h6 M9 16h3", false) }
        val PhotoCamera: ImageVector by lazy { line("PhotoCamera", "M4 8h3l1.5-2h7L17 8h3v11H4z M12 10.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z", false) }
        val FolderOpen: ImageVector by lazy { line("FolderOpen", "M3 7a1 1 0 0 1 1-1h5l2 2h8a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z", false) }
        val CalendarMonth: ImageVector by lazy { line("CalendarMonth", "M4 6h16v14H4z M4 10h16 M8 3v4 M16 3v4", false) }
        val Storefront: ImageVector by lazy { line("Storefront", "M4 9l1.5-5h13L20 9 M4 9a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 6 0 2.5 2.5 0 0 0 5 0 M5 12v8h14v-8 M10 20v-4h4v4", false) }
        val Sort: ImageVector by lazy { line("Sort", "M4 7h16 M7 12h10 M10 17h4", false) }
        val Share: ImageVector by lazy { line("Share", "M12 4v11 M8 8l4-4 4 4 M5 13v6h14v-6", false) }
        val Settings: ImageVector by lazy { line("Settings", "M4 7h9 M17 7h3 M4 17h3 M11 17h9 M15 4.5v5 M9 14.5v5", false) }
        val Restaurant: ImageVector by lazy { line("Restaurant", "M7 3v8 M5 3v5a2 2 0 0 0 4 0V3 M7 11v10 M16 21V3c-2.2 1.5-3 4-3 7h3", false) }
        val Policy: ImageVector by lazy { line("Policy", "M12 3l7 3v5.5c0 4.2-2.9 7.7-7 9-4.1-1.3-7-4.8-7-9V6z M9.5 11h5 M9.5 14h3", false) }
        val Payments: ImageVector by lazy { line("Payments", "M3 7h18v10H3z M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z", false) }
        val Numbers: ImageVector by lazy { line("Numbers", "M9 4L7 20 M17 4l-2 16 M5 9h15 M4 15h15", false) }
        val MedicalServices: ImageVector by lazy { line("MedicalServices", "M5 5h14v14H5z M12 8.5v7 M8.5 12h7", false) }
        val Lock: ImageVector by lazy { line("Lock", "M6 11h12v9H6z M8.5 11V8a3.5 3.5 0 0 1 7 0v3", false) }
        val LocalGroceryStore: ImageVector by lazy { line("LocalGroceryStore", "M3 4h2.5l2 10h10l2-7H6.5 M9 19.5a.5.5 0 1 0 .01 0 M17 19.5a.5.5 0 1 0 .01 0", false) }
        val LocalGasStation: ImageVector by lazy { line("LocalGasStation", "M5 20V5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v15 M4 20h11 M7 8h5 M14 9h2.5a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 0 3 0V8l-2.5-2.5", false) }
        val Kitchen: ImageVector by lazy { line("Kitchen", "M7 3h10v18H7z M7 10h10 M10 6v2 M10 13v3", false) }
        val Image: ImageVector by lazy { line("Image", "M4 5h16v14H4z M4 16l5-5 4 4 2-2 5 5 M15 9.5a.5.5 0 1 0 .01 0", false) }
        val HourglassBottom: ImageVector by lazy { line("HourglassBottom", "M7 3h10 M7 21h10 M8 3v3.5L12 12 8 17.5V21 M16 3v3.5L12 12l4 5.5V21", false) }
        val Flight: ImageVector by lazy { line("Flight", "M21 4L3 11l7 3 3 7z M10 14l11-10", false) }
        val ErrorOutline: ImageVector by lazy { line("ErrorOutline", "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z M12 8v5 M12 16.2v.01", false) }
        val Edit: ImageVector by lazy { line("Edit", "M4 20h4L19 9l-4-4L4 16z M13.5 6.5l4 4", false) }
        val DirectionsCar: ImageVector by lazy { line("DirectionsCar", "M5 16v-5l2-5h10l2 5v5 M3 16h18 M7 19v-3 M17 19v-3 M7.5 13h.01 M16.5 13h.01", false) }
        val Devices: ImageVector by lazy { line("Devices", "M3 5h18v11H3z M9 20h6 M12 16v4", false) }
        val Delete: ImageVector by lazy { line("Delete", "M5 7h14 M9 7V4h6v3 M7 7l1 13h8l1-13 M10 11v6 M14 11v6", false) }
        val CurrencyRupee: ImageVector by lazy { line("CurrencyRupee", "M7 5h10 M7 9h10 M7 5h2.5a4 4 0 0 1 0 8H7l8 7", false) }
        val Checkroom: ImageVector by lazy { line("Checkroom", "M8 4L3 7l2.5 4L8 10v10h8V10l2.5 1L21 7l-5-3a4 4 0 0 1-8 0z", false) }
        val Chair: ImageVector by lazy { line("Chair", "M5 11V8a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v3 M3 13a2 2 0 0 1 4 0v1h10v-1a2 2 0 0 1 4 0v5H3z M6 18v2 M18 18v2", false) }
        val Bolt: ImageVector by lazy { line("Bolt", "M13 3L5 13h6l-1 8 8-10h-6z", false) }
        val AddPhotoAlternate: ImageVector by lazy { line("AddPhotoAlternate", "M13 5H4v14h16v-9 M4 16l5-5 4 4 2-2 5 5 M17 3v6 M14 6h6", false) }
        val AccountCircle: ImageVector by lazy { line("AccountCircle", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M12 6.5a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M6.5 18c1-2.5 3-3.5 5.5-3.5s4.5 1 5.5 3.5", false) }
        val Pdf: ImageVector by lazy { line("Pdf", "M7 3h7l5 5v13H7z M14 3v5h5 M9.5 14h6 M9.5 17h4", false) }
        val Bell: ImageVector by lazy { line("Bell", "M6 17v-6a6 6 0 0 1 12 0v6l1.5 2h-15z M10 21h4", false) }
        val Download: ImageVector by lazy { line("Download", "M12 4v11 M8 11l4 4 4-4 M5 19h14", false) }
        val ChevronRight: ImageVector by lazy { line("ChevronRight", "M9 6l6 6-6 6", true) }
        val Notifications: ImageVector by lazy { line("Notifications", "M6 17v-6a6 6 0 0 1 12 0v6l1.5 2h-15z M10 21h4", false) }
    }
    object AutoMirrored {
        object Rounded {
            val ArrowBack: ImageVector by lazy { line("ArrowBackMirrored", "M19 12H5 M11 6l-6 6 6 6", true) }
        }
    }
}

private fun line(name: String, d: String, mirror: Boolean): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror).apply {
        addPath(
            pathData = addPathNodes(d), fill = null,
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
