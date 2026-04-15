import pymysql 
import os
# 不需要 dotenv 了，因為我們直接指定內容

def get_db_connection():
    """
    建立與 MySQL 資料庫的連線。
    根據你的 Workbench 截圖，資料庫名稱為 plant_db。
    """
    return pymysql.connect(
        host = "127.0.0.1",       # 本機伺服器
        user = "root",            # 預設帳號
        password = "",            # 如果你有設密碼請填入，否則留空
        database = "plant_db",    # 你的資料庫名稱
        cursorclass = pymysql.cursors.DictCursor
    )

def get_or_create_id(table_name, name_val, target_crop_id, description=None, treatment=None):
    """
    檢查名稱是否存在，若無則新增。
    支援同時存入描述與治療建議。
    """
    # 1. 定義欄位映射表
    column_mapping = {
        "disease": {"id": "disease_id", "name": "disease_name"},
        "pests": {"id": "pest_id", "name": "pest_name"}
    }
    
    cols = column_mapping.get(table_name, {"id": "id", "name": "name"})
    id_col = cols["id"]
    name_col = cols["name"]

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # 檢查是否存在
            check_sql = f"SELECT {id_col} FROM {table_name} WHERE {name_col} = %s AND crop_id = %s"
            cursor.execute(check_sql, (name_val, target_crop_id))
            result = cursor.fetchone()
            
            if result:
                return result[id_col]
            
            # 不存在則新增
            insert_sql = f"""
                INSERT INTO {table_name} ({name_col}, crop_id, description, treatment)
                VALUES (%s, %s, %s, %s)
            """
            cursor.execute(insert_sql, (name_val, target_crop_id, description, treatment))
            conn.commit()
            return cursor.lastrowid
    except Exception as e:
        print(f"資料庫操作失敗: {e}")
        return None
    finally:
        conn.close()

def get_crop_id_by_name(crop_name):
    """取得特定作物的 ID"""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "SELECT crop_id FROM crop WHERE crop_name = %s"
            cursor.execute(sql, (crop_name,))
            result = cursor.fetchone()
            if result:
                return result["crop_id"]
            else:
                print(f"⚠️ 警告：crop 表找不到名為 '{crop_name}' 的植物")
                return None
    finally:
        conn.close()