#Running PC: COMP Depatyment’s LAB PC
IDE: VsCode (Visual Studio Code)
Java Versison: JDK17
Lib:ojdbc8
Encryption_Key:encryption_key.key

*you can run on COMP LAB PC to connect the Oracle DB*
https://intranet.comp.polyu.edu.hk/TechServices/TechnicalTips/881
https://puuds.polyu.edu.hk/uds/page/services

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
 