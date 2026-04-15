import streamlit as st
import pandas as pd
from database import SessionLocal
from models import Crop  # 暫時只匯入 Crop，刪掉 Diary

st.set_page_config(page_title="植物診斷管理後台", layout="wide")
st.title("🌿 植物診斷系統管理中心")

db = SessionLocal()

menu = st.sidebar.selectbox("管理功能", ["數據概覽", "作物百科庫"])

if menu == "數據概覽":
    st.header("📊 系統運作統計")
    total_crops = db.query(Crop).count()
    st.metric("系統收錄作物", f"{total_crops} 種")
    st.info("診斷紀錄功能將在資料表建立後開放。")

elif menu == "作物百科庫":
    st.header("🌾 現有作物清單")
    crops = db.query(Crop).all()
    df = pd.DataFrame([{"ID": c.crop_id, "作物名稱": c.crop_name, "英文名": c.crop_name_en} for c in crops])
    st.dataframe(df, use_container_width=True)

db.close()