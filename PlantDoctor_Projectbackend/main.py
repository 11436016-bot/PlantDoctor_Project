from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException
from fastapi.staticfiles import StaticFiles
import shutil
import os
import uvicorn
import jwt
import datetime
from pathlib import Path
from pydantic import BaseModel, EmailStr
from passlib.context import CryptContext
from db_utils import get_db_connection
from test_gemini import save_to_db, diagnostic_plant
from auth import get_current_user, verify_admin

app = FastAPI(title="植物病害診斷系統 API")

# 掛載靜態檔案
app.mount("/static", StaticFiles(directory="static"), name="static")

# 設定上傳目錄
UPLOAD_DIR = Path("static/uploads")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

# --- 重要：安全設定 (與 auth.py 必須完全一致) ---
SECRET_KEY = "jerry_secure_key_2026_plant_doctor_project_final_check"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24

# 密碼加密工具
pwd_context = CryptContext(schemes=["bcrypt"], bcrypt__ident="2b")

class UserRegister(BaseModel):
    username: str
    password: str
    email: EmailStr
    full_name: str = None
    role: str = "user"

class UserLogin(BaseModel):
    username: str
    password: str

@app.get("/")
def read_root():
    return {"message": "後端伺服器運作中！"}

# 1. 註冊帳戶
@app.post("/users/register")
async def register_user(user: UserRegister):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            check_sql = "SELECT user_id FROM user WHERE username = %s OR email = %s"
            cursor.execute(check_sql, (user.username, user.email))
            if cursor.fetchone():
                raise HTTPException(status_code=400, detail="帳號或 Email 已存在")
            
            hashed_password = pwd_context.hash(user.password)
            insert_sql = "INSERT INTO user (username, password_hash, email, full_name, role) VALUES (%s, %s, %s, %s, %s)"
            cursor.execute(insert_sql, (user.username, hashed_password, user.email, user.full_name, user.role))
            conn.commit()
            return {"status": "success", "message": "註冊成功"}
    finally:
        conn.close()

# 2. 帳戶登入
@app.post("/user/login")
async def login_user(user: UserLogin):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "SELECT user_id, username, password_hash, role FROM user WHERE username = %s"
            cursor.execute(sql, (user.username,))
            db_user = cursor.fetchone()

            if not db_user or not pwd_context.verify(user.password, db_user["password_hash"]):
                raise HTTPException(status_code=400, detail="帳號或密碼錯誤")
            
            expire = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
            payload = {
                "user_id": db_user["user_id"],
                "username": db_user["username"],
                "role": db_user["role"],
                "exp": expire
            }
            token = jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)
            return {"status": "success", "access_token": token, "token_type": "bearer"}
    finally:
        conn.close()

# 3. 上傳照片並診斷
@app.post("/diaries/upload")
async def create_diary(
    user_note: str = Form(""),
    file: UploadFile = File(...),
    current_user: dict = Depends(get_current_user)
):
    user_id = current_user["user_id"]
    file_path = UPLOAD_DIR / file.filename
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    db_path = file_path.as_posix()
    result = diagnostic_plant(db_path)
    
    if result:
        full_url = f"http://10.0.2.2:8000/{db_path}"
        my_id = save_to_db(result, file_path, user_id, user_note)
        return {"status": "success", "data": result}
    return {"status": "error", "message": "AI 辨識失敗"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)