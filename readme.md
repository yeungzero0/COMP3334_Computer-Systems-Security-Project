# COMP3334 Computer Systems Security - Group Project (Team89)

[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.java.com/)
[![Oracle](https://img.shields.io/badge/Oracle-DB-red)](https://www.oracle.com/database/)
[![AES-GCM](https://img.shields.io/badge/Encryption-AES--GCM-green)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![TOTP 2FA](https://img.shields.io/badge/2FA-TOTP-orange)](https://en.wikipedia.org/wiki/Time-based_One-time_Password)

**POLYU COMP3334 Group Project**  
一個**安全的檔案共享系統**，具備使用者註冊/登入、TOTP 雙重驗證、AES-GCM 檔案加密、檔案上傳/下載/分享/刪除、系統日誌及管理員功能。

---

## ⚠️ 免責聲明 (Disclaimer)

**本專案純粹用於學習與課程用途**。

- 此專案是 **POLYU COMP3334 (2025)** 的課程作業，主要目的是展示電腦系統安全技術的應用（密碼雜湊、TOTP 2FA、檔案加密、資料庫安全等）。
- **嚴禁商業使用**：本專案**不得**用於任何商業活動、產品開發或生產環境。
- **僅供參考與教育**：所有程式碼、資料與報告僅供個人學習、測試與參考之用。
- **無任何保證**：作者不對使用本專案所產生的任何輸出、系統異常或後果承擔任何責任。

> **總之：這只是我們在 POLYU 學習電腦系統安全的課程專案，不是專業商用解決方案。**

---

## 專案特色

- **使用者認證**：支援帳號/Email 登入 + PBKDF2 密碼雜湊 + **TOTP 雙重驗證**
- **檔案安全**：上傳前使用 **AES-256-GCM** 加密，確保檔案在傳輸與儲存過程中的機密性
- **檔案管理**：上傳、下載、分享、取消分享、刪除檔案
- **管理員功能**：查看系統日誌、鎖定/解鎖帳戶
- **客戶端-伺服器架構**：Socket 通訊 + Oracle 資料庫後端
- **安全性設計**：防止 SQL Injection、檔案加密、TOTP 防重放攻擊等

---

## Demo
https://youtu.be/b-zQLcVsL_U

---
## Setup  
#Running PC: COMP Depatyment’s LAB PC  
IDE: VsCode (Visual Studio Code)  
Java Versison: JDK17  
Lib:ojdbc8  
Encryption_Key:encryption_key.key  
  
#Run .java
1. open your VsCode  
2. drag and drop the project folder's "code" into the VsCode  
3. ensure it is installed the java extension ofr your VsCode  
4. run Server.java   
5. run Client.java  
  
#Setup SQL (if you want to login your oracle acc, otherwise the java will using 22027226d as the oracle DB)  
1. open SetUpDB_SQL.docx  
2. login your SQL account  
3. copy and paste all the SQL  
  
or import the database_setup.sql into your SQLPlus with your account. If used demo DB, no need to reset/import .sql file  
  
1. open the sqlplus  
2. login your oracle acc  
3. enter "@/path/to/your/database_setup.sql" (e.g: SQL> @J:\comp3334\Team3334_JavaCLI\database_setup.sql)  
  
simple account:  
1. username:yyy, pwd:x0  
2. username:yyy2, pwd:x0   
3. username:badacc, pwd:x0 (locked account)  
4. username:admin, pwd:x0 (admin account)  
 
