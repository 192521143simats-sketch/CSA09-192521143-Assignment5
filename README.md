# CSA09 Programming in Java — Assignment 5

**Name:** R Sarvesh  
**Register number:** 192521143  
**Java:** Oracle JDK 21.0.12.1  
**Database:** MySQL 8.4.9 with Connector/J 8.4.0

The Word and PDF reports contain the Part A pseudocode, Part B results, and execution screenshots. Each task folder contains its Java source and an output screenshot.

## Compile and run

From the repository root in PowerShell, place the [MySQL Connector/J 8.4.0 JAR](https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar) in the current directory, or substitute its absolute path. Compile the sources together because several tasks share the file parser and connection helper.

```powershell
New-Item -ItemType Directory -Force classes | Out-Null
$src = Get-ChildItem Task* -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d classes $src
$cp = 'classes;mysql-connector-j-8.4.0.jar'
```

For A3, A5 and B2–B4, use a dedicated local MySQL instance. Task B3 truncates its benchmark table between runs. From a MySQL client session, execute the schema script with `SOURCE TaskB3/setup.sql`. Set the following session variables for your test server:

```powershell
$env:ASSIGNMENT5_URL = 'jdbc:mysql://127.0.0.1:33405/csa09_assignment5?rewriteBatchedStatements=true'
$env:ASSIGNMENT5_USER = 'your_test_user'
$env:ASSIGNMENT5_PASSWORD = 'your_test_password'
```

| Task | Run from repository root | Result |
|---|---|---|
| A1 | `java -cp $cp LayoutDemo` | The encoded delimiter round-tripped and the route query returned one pass. |
| A2 | `java -cp $cp ReadCost` | 52,428,801 single-byte calls versus 801 full-block calls, including EOF. |
| A3 | `java -cp $cp DaoDemo` | Student insert, lookup, update, list and deletion succeeded. |
| A4 | `java -cp $cp HostelSnapshot` | Transient state was omitted and the shared resident reference was preserved. |
| A5 | `java -cp $cp WalletDemo` | Rollback preserved both balances. Commit moved 50,000 paise and preserved the total. |
| B1 | `java -cp $cp PassStore` then `java -cp $cp FileExperiment` | Template output matched. File validation and timing results are in the report. The 50 MiB byte-at-a-time run can take several minutes. |
| B2 | `java -cp $cp LoginCheck TaskB2/damaged-passes.txt` | Bound login values behaved as expected and three damaged lines were rejected. |
| B3 | `java -cp $cp InsertExperiment` | All methods passed SELECT verification. A forced failure left 2,000 rows with auto-commit and zero with rollback. |
| B4 | `java -cp $cp MigratePasses TaskB4/legacy-passes.txt` | On an empty destination table, two passes were committed and five records were rejected. |

