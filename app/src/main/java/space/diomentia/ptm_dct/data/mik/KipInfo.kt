package space.diomentia.ptm_dct.data.mik

import java.time.LocalDate

data class KipInfo(
    val organizationName: String,
    val area: String,
    val pipeline: String,
    val anchorPoint: Float,
    val kipName: String,
    val commissioningDate: LocalDate,
    val producer: String,
    val passportLink: String
)

val demoKipData = mapOf<String, KipInfo>(
    "7fbd" to KipInfo(
        organizationName = "ООО \"Газпром трансгаз Саратов\"",
        area = "Александровогайское ЛПУ МГ",
        pipeline = "МГ САЦ-2 (1145-1224 км)",
        anchorPoint = 1168.4f,
        kipName = "КИП.ПТМ.2.2.8-6.БСЗ.20-2.УХЛ1",
        commissioningDate = LocalDate.of(2024, 6, 25),
        producer = "ООО НПК \"Промтехмастер\"",
        passportLink = ""
    ),
    "7f3c" to KipInfo(
        organizationName = "ООО \"Газпром трансгаз Саратов\"",
        area = "Александровогайское ЛПУ МГ",
        pipeline = "МГ САЦ-2 (1145-1224 км)",
        anchorPoint = 1182.3f,
        kipName = "КИП.ПТМ.2.2В.12-4.БСЗ.20-4.УХЛ1",
        commissioningDate = LocalDate.of(2024, 6, 25),
        producer = "ООО НПК \"Промтехмастер\"",
        passportLink = ""
    ),
    "6132" to KipInfo(
        organizationName = "ООО \"Газпром трансгаз Саратов\"",
        area = "Александровогайское ЛПУ МГ",
        pipeline = "МГ САЦ-2 (1145-1224 км)",
        anchorPoint = 1220.2f,
        kipName = "КИП.ПТМ.2.2.6-4.БСЗ.10-2.УХЛ1",
        commissioningDate = LocalDate.of(2024, 6, 25),
        producer = "ООО НПК \"Промтехмастер\"",
        passportLink = ""
    )
)