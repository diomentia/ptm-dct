package space.diomentia.ptm_dct.data

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import space.diomentia.ptm_dct.R
import space.diomentia.ptm_dct.data.mik.MikState
import space.diomentia.ptm_dct.ui.theme.blue_aquamarine
import space.diomentia.ptm_dct.ui.theme.blue_zodiac
import space.diomentia.ptm_dct.ui.theme.white
import java.time.format.DateTimeFormatter

fun MikState.exportJournalToExcel(
    context: Context
): Workbook? {
    if (journal.isEmpty())
        return null
    fun toXssfColor(color: Color) = XSSFColor(color.toArgb().let { argb ->
        arrayOf((argb shr 4) and 0xFF, (argb shr 2) and 0xFF, argb and 0xFF)
            .map { it.toByte() }.toByteArray()
    })
    val workbook = XSSFWorkbook()
    val headerStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        setFillBackgroundColor(toXssfColor(blue_aquamarine))
        setFillForegroundColor(toXssfColor(Color.Black))
        setFont(workbook.createFont().apply {
            bold = true
        })
    }
    val bodyStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.LEFT
        setFillBackgroundColor(toXssfColor(blue_zodiac))
        setFillForegroundColor(toXssfColor(white))
    }

    workbook.createSheet("MIK-${authInfo?.serialNumber ?: "??"}").run {
        val headers = arrayOf(
            context.getString(R.string.date),
            context.getString(R.string.time),
            context.getString(R.string.battery),
            context.getString(R.string.controller_temperature),
            *(1..4).map { "${context.getString(R.string.channel)} $it" }.toTypedArray()
        )
        createRow(0).run {
            headers.forEachIndexed { i, header ->
                createCell(i).run {
                    setCellValue(header)
                    setCellStyle(headerStyle)
                }
            }
        }
        journal.forEachIndexed { i, entry ->
            createRow(i + 1).run {
                createCell(0).run {
                    setCellValue(entry.timestamp.format(DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy"
                    )))
                    cellStyle = bodyStyle
                }
                createCell(1).run {
                    setCellValue(entry.timestamp.format(DateTimeFormatter.ofPattern(
                        "hh:mm:ss"
                    )))
                    cellStyle = bodyStyle
                }
                createCell(2).run {
                    setCellValue(entry.battery.toDouble())
                    cellStyle = bodyStyle
                }
                createCell(3).run {
                    setCellValue(entry.controllerTemperature.toDouble())
                    cellStyle = bodyStyle
                }
                entry.voltage.forEachIndexed { i, v ->
                    createCell(4 + i).run {
                        setCellValue(v.toDouble())
                        cellStyle = bodyStyle
                    }
                }
            }
        }
    }
    return workbook
}