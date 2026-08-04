# -*- coding: utf-8 -*-
GAN = ["甲","乙","丙","丁","戊","己","庚","辛","壬","癸"]
ZHI = ["子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"]

# DiZhi.entries order: ZI,CHOU,YIN,MAO,CHEN,SI,WU,WEI,SHEN,YOU,XU,HAI
# TianGan.entries order: JIA..GUI

JIE = [
    (1,6,11),(2,4,0),(3,6,1),(4,5,2),(5,6,3),(6,6,4),
    (7,7,5),(8,8,6),(9,8,7),(10,8,8),(11,7,9),(12,7,10)
]

def month_zhi(m,d):
    md = m*100+d
    idx = 10
    for (jm,jd,zi) in JIE:
        if jm*100+jd <= md: idx = zi
    return (idx+2)%12

def year_gz(y,m,d):
    gz_year = y-1 if (m<2 or (m==2 and d<4)) else y
    seq = ((gz_year-4)%60+60)%60
    return seq%10, seq%12

def day_gz(y,m,d):
    # days diff from 2000-01-01 (戊午 = gan5,zhi6 -> seq 54)
    import datetime
    base = datetime.date(2000,1,1)
    target = datetime.date(y,m,d)
    diff = (target - base).days
    seq = ((54+diff)%60+60)%60
    return seq%10, seq%12

def month_gz(year_gan_idx, month_zhi_idx):
    start = {0:2,1:4,2:6,3:8,4:0,5:2,6:4,7:6,8:8,9:0}[year_gan_idx]
    month_seq = (month_zhi_idx - 2 + 12)%12
    return (start+month_seq)%10

def hour_gz(day_gan_idx, hour):
    zhi_idx = ((hour+1)//2)%12
    start = {0:0,1:2,2:4,3:6,4:8,5:0,6:2,7:4,8:6,9:8}[day_gan_idx]
    return (start+zhi_idx)%10, zhi_idx

def four(y,m,d,h):
    yg_gan,yg_zhi = year_gz(y,m,d)
    mz = month_zhi(m,d)
    mg_gan = month_gz(yg_gan, mz)
    dg_gan,dg_zhi = day_gz(y,m,d)
    hg_gan,hz = hour_gz(dg_gan, h)
    return {
        "年": GAN[yg_gan]+ZHI[yg_zhi],
        "月": GAN[mg_gan]+ZHI[mz],
        "日": GAN[dg_gan]+ZHI[dg_zhi],
        "时": GAN[hg_gan]+ZHI[hz],
    }

tests = [
    (2000,1,1,0),    # 己卯年 戊午日；0时子时: 戊日->壬子
    (2000,2,5,12),   # 庚辰年
    (2024,2,10,10),  # 甲辰年
    (2026,8,3,15),   # 今天
    (1984,6,1,8),    # 甲子年? 1984立春后=甲子年
]
for t in tests:
    print(t, four(*t))
