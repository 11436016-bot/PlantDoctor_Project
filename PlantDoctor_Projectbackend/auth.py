from fastapi import Depends, HTTPException
import jwt
from fastapi.security import HTTPBearer
from db_utils import get_db_connection

security = HTTPBearer()

# 務必確認這裡的 KEY 與 main.py 一致
SECRET_KEY = "jerry_secure_key_2026_plant_doctor_project_final_check"
ALGORITHM = "HS256"

async def get_current_user(authorization: str = Depends(security)):
    token = authorization.credentials.strip()
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id = payload.get("user_id")
        
        if user_id is None:
            raise HTTPException(status_code=401, detail="無效 Token")
            
        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                sql = "SELECT user_id, username, role FROM user WHERE user_id = %s"
                cursor.execute(sql, (user_id,))
                user = cursor.fetchone()
                if not user:
                    raise HTTPException(status_code=401, detail="使用者不存在")
                return user
        finally:
            conn.close()
            
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token 已過期")
    except Exception as e:
        print(f"DEBUG - 驗證失敗細節: {str(e)}")
        raise HTTPException(status_code=401, detail="身分驗證失敗")

# 就是這一段！剛才可能漏掉了
async def verify_admin(current_user: dict = Depends(get_current_user)):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="權限不足，只有管理人員可以執行操作")
    return current_user