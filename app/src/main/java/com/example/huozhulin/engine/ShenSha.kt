package com.example.huozhulin.engine

import com.example.huozhulin.data.model.DiZhi
import com.example.huozhulin.data.model.TianGan

/** 神煞（以日干支为主，年与部分神煞为辅） */
object ShenSha {

    /** 驿马：日支三合局之长生对宫。
     *  申子辰马在寅；寅午戌马在申；巳酉丑马在亥；亥卯未马在巳。
     */
    fun yiMa(dayZhi: DiZhi): DiZhi = when (dayZhi) {
        DiZhi.SHEN, DiZhi.ZI, DiZhi.CHEN -> DiZhi.YIN
        DiZhi.YIN, DiZhi.WU, DiZhi.XU -> DiZhi.SHEN
        DiZhi.SI, DiZhi.YOU, DiZhi.CHOU -> DiZhi.HAI
        DiZhi.HAI, DiZhi.MAO, DiZhi.WEI -> DiZhi.SI
    }

    /** 桃花（咸池）：日支三合局之沐浴位。
     *  申子辰桃花在酉；寅午戌桃花在卯；巳酉丑桃花在午；亥卯未桃花在子。
     */
    fun taoHua(dayZhi: DiZhi): DiZhi = when (dayZhi) {
        DiZhi.SHEN, DiZhi.ZI, DiZhi.CHEN -> DiZhi.YOU
        DiZhi.YIN, DiZhi.WU, DiZhi.XU -> DiZhi.MAO
        DiZhi.SI, DiZhi.YOU, DiZhi.CHOU -> DiZhi.WU
        DiZhi.HAI, DiZhi.MAO, DiZhi.WEI -> DiZhi.ZI
    }

    /** 干禄：日干之禄。
     *  甲禄寅、乙禄卯、丙戊禄巳、丁己禄午、庚禄申、辛禄酉、壬禄亥、癸禄子。
     */
    fun ganLu(dayGan: TianGan): DiZhi = when (dayGan) {
        TianGan.JIA -> DiZhi.YIN
        TianGan.YI -> DiZhi.MAO
        TianGan.BING, TianGan.WU -> DiZhi.SI
        TianGan.DING, TianGan.JI -> DiZhi.WU
        TianGan.GENG -> DiZhi.SHEN
        TianGan.XIN -> DiZhi.YOU
        TianGan.REN -> DiZhi.HAI
        TianGan.GUI -> DiZhi.ZI
    }

    /** 贵人（天乙贵人）：日干查贵人。
     *  甲戊庚牛羊；乙己鼠猴乡；丙丁猪鸡位；壬癸蛇兔藏；六辛逢马虎。
     */
    fun guiRen(dayGan: TianGan): List<DiZhi> = when (dayGan) {
        TianGan.JIA, TianGan.WU, TianGan.GENG -> listOf(DiZhi.CHOU, DiZhi.WEI)
        TianGan.YI, TianGan.JI -> listOf(DiZhi.ZI, DiZhi.SHEN)
        TianGan.BING, TianGan.DING -> listOf(DiZhi.HAI, DiZhi.YOU)
        TianGan.REN, TianGan.GUI -> listOf(DiZhi.SI, DiZhi.MAO)
        TianGan.XIN -> listOf(DiZhi.WU, DiZhi.YIN)
    }
}
