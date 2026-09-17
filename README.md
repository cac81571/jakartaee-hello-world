# Jakarta EE 10 / Open Liberty CRUD サンプル

Eclipse Starter の Hello World をベースに、Oracle 上の `ITEMS` テーブルを CRUD する Web アプリです。

元の Starter 説明は [README.eclipse-starter.md](README.eclipse-starter.md) にあります。

## 必要なもの

- Java 21（`JAVA_HOME` を Java 21 にする）
- Maven は不要（`mvnw.cmd` 同梱）
- 接続できる Oracle Database（12c 以降）
- テーブル作成権限のあるスキーマユーザー

## Oracle の準備

1. 接続情報を `src/main/liberty/config/bootstrap.properties` に書く。

```
oracle.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
oracle.user=app
oracle.password=app
```

サービス名は環境に合わせて `XEPDB1` や `ORCLPDB1` などに変更する。

2. テーブルを用意する。MyBatis はテーブルを自動作成しないので、`src/main/sql/items.sql` を実行する。

```sql
CREATE TABLE ITEMS (
    ID NUMBER(19) PRIMARY KEY,
    NAME VARCHAR2(100) NOT NULL,
    DESCRIPTION VARCHAR2(500)
);
```

`ID` はシーケンスや IDENTITY では採番しない。画面または API から直接指定する。

以前シーケンス／IDENTITY 付きで作ったテーブルがある場合は作り直す。

```sql
DROP TABLE ITEMS;
DROP SEQUENCE ITEMS_SEQ;
```

## テーブルを変更するとき（参考）

このアプリは **POJO・MyBatis マッパー・画面** が同じ列を共有している。DB だけ変えても動かない。

いま紐づいている場所:

| 場所 | 内容 |
| --- | --- |
| `src/main/sql/items.sql` | DDL の正 |
| `Item.java` | 結果のマッピング（POJO） |
| `ItemMapper.java` | SELECT / INSERT / UPDATE / DELETE |
| `src/main/webapp/index.html` | 入力欄と JSON |
| `ItemResource.java` | 必須チェック（いまは `id` と `name`） |

開発中のテーブル変更は SQL で明示する。MyBatis はスキーマを自動では変えない。

### 列を足す例（`STATUS`）

1. サーバーを止める（`Ctrl+C` または `.\mvnw.cmd liberty:stop`）。
2. Oracle で ALTER する（既存データを残す場合）。

```sql
ALTER TABLE ITEMS ADD STATUS VARCHAR2(20);
```

3. `src/main/sql/items.sql` の `CREATE TABLE` にも同じ列を足す（次から新規作成するとき用）。
4. `Item.java` にフィールドと getter / setter を足す。
5. `ItemMapper.java` の SELECT / INSERT / UPDATE に列を足す。`#{status}` のようにプロパティ名でバインドする。

6. 画面の入力・一覧・`payload()` に `status` を足す。API の JSON も同じプロパティ名になる。
7. サーバーを再起動する。`.\mvnw.cmd liberty:run`

### 列を消す・型を変える・作り直す

ALTER で落とすか、開発用ならドロップして作り直す。

```sql
DROP TABLE ITEMS;
```

そのあと `src/main/sql/items.sql` を実行する。`Item.java` と `ItemMapper` の SQL から消した列を抜かないと、INSERT / UPDATE が失敗する。

### テーブル名を変える

`ITEMS` は次に直書きされている。

- `ItemMapper.java` の SQL
- `src/main/sql/items.sql`

### 確認

起動後に画面で追加できること、および `GET /rest/items` で新しい列が JSON に出ることを見る。古い HTML が出るときは `Ctrl+F5`。

## 起動と停止

プロジェクトディレクトリで実行する（Windows）。

```powershell
.\mvnw.cmd liberty:run
```

初回は Open Liberty と Oracle JDBC ドライバ（`ojdbc11` 23.26.2.0.0）の取得がある。

起動後:

- 画面: http://localhost:9080/
- REST のベース: http://localhost:9080/rest/

停止:

- `liberty:run` のターミナルで `Ctrl+C`
- または別ターミナルで `.\mvnw.cmd liberty:stop`

設定や Java を変えたあとは再起動する。ブラウザに古い Welcome ページが出る場合は `Ctrl+F5` か http://localhost:9080/?v=2 を開く。

Windows のコンソールが文字化けする場合は、ターミナルを開き直してから `mvnw.cmd` を使う（UTF-8 / `chcp 65001`）。

## 画面の使い方

http://localhost:9080/ の「アイテム管理」で次ができる。

| 操作 | 手順 |
| --- | --- |
| 追加 | ID・名前（必須）・説明を入力して「追加」 |
| 更新 | 行の「編集」→ 名前／説明を直して「更新」（ID は変更不可） |
| 削除 | 行の「削除」 |
| クリア | 入力欄を空にする |

一覧が取れないときは Oracle 接続と `ITEMS` テーブルを確認する。

## REST API

### Hello

```
GET /rest/hello
GET /rest/hello?name=Jane
```

例: http://localhost:9080/rest/hello → `{"hello":"world"}`

### アイテム

| メソッド | パス | 内容 |
| --- | --- | --- |
| GET | `/rest/items` | 一覧 |
| GET | `/rest/items/{id}` | 1 件取得 |
| POST | `/rest/items` | 追加（`id` と `name` 必須） |
| PUT | `/rest/items/{id}` | 更新（`name` 必須） |
| DELETE | `/rest/items/{id}` | 削除 |

追加の例:

```powershell
curl.exe -X POST http://localhost:9080/rest/items -H "Content-Type: application/json" -d "{\"id\":1,\"name\":\"メモ\",\"description\":\"説明\"}"
```

更新の例:

```powershell
curl.exe -X PUT http://localhost:9080/rest/items/1 -H "Content-Type: application/json" -d "{\"name\":\"更新後\",\"description\":\"説明\"}"
```

CRUD の SQL は `ItemMapper` に書き、`ItemService` が MyBatis の `SqlSession` で実行する。トランザクションは Jakarta `@Transactional`（JTA）に任せる。

```java
SELECT ID, NAME, DESCRIPTION FROM ITEMS ORDER BY ID
SELECT ID, NAME, DESCRIPTION FROM ITEMS WHERE ID = #{id}
INSERT INTO ITEMS (ID, NAME, DESCRIPTION) VALUES (#{id}, #{name}, #{description})
UPDATE ITEMS SET NAME = #{name}, DESCRIPTION = #{description} WHERE ID = #{id}
DELETE FROM ITEMS WHERE ID = #{id}
```

プレースホルダは MyBatis の `#{プロパティ名}`。テーブルや列を変えたらこの SQL と `Item.java` を揃える。

DataSource は `SqlSessionFactoryProducer` が JNDI `jdbc/oracle` を探す。

## JBoss EAP 8.1 で動かす（参考）

アプリの Java コードは Jakarta EE 10 なので **EAP 8.1 + Java 21** で動かせる。ただし今の `liberty:run` と `server.xml` は Open Liberty 専用なので、EAP 側で DataSource とデプロイを用意する。EAP の HTTP は既定 **8080** である。

### 1. WAR を作る

```powershell
.\mvnw.cmd package
```

成果物は `target/jakartaee-hello-world.war`。

### 2. Oracle JDBC をモジュールにする

EAP の `jboss-cli.bat` を使う例（`ojdbc11.jar` のパスは環境に合わせる）。

```
module add --name=com.oracle.ojdbc --resources=C:\path\to\ojdbc11.jar --dependencies=jakarta.api,jakarta.transaction.api
```

続けてドライバを登録する。

```
/subsystem=datasources/jdbc-driver=oracle:add(driver-name=oracle,driver-module-name=com.oracle.ojdbc,driver-class-name=oracle.jdbc.OracleDriver,driver-datasource-class-name=oracle.jdbc.datasource.OracleDataSource)
```

### 3. DataSource を作る

```
data-source add --name=OracleDS --jndi-name=java:jboss/datasources/OracleDS --driver-name=oracle --connection-url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1 --user-name=app --password=app --jta=true --use-ccm=true --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker --exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter
```

URL・ユーザーは `bootstrap.properties` と同じ値にする。

### 4. JNDI をアプリに合わせる

MyBatis は JNDI 名 `jdbc/oracle` を参照する。EAP では `src/main/webapp/WEB-INF/jboss-web.xml` で `java:jboss/datasources/OracleDS` に結線する。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-web>
    <context-root>/</context-root>
    <resource-ref>
        <res-ref-name>jdbc/oracle</res-ref-name>
        <jndi-name>java:jboss/datasources/OracleDS</jndi-name>
    </resource-ref>
</jboss-web>
```

`context-root` を付けない場合、URL は `http://localhost:8080/jakartaee-hello-world/` になる。

### 5. テーブルを用意する

MyBatis はテーブルを作らない。`src/main/sql/items.sql` を SQL\*Plus などから実行する。

### 6. デプロイする

```
deploy /path/to/jakartaee-hello-world/target/jakartaee-hello-world.war
```

または WAR を `EAP_HOME/standalone/deployments/` にコピーする。

確認:

- 画面: http://localhost:8080/ （`jboss-web.xml` で `/` にした場合）
- Hello: http://localhost:8080/rest/hello
- アイテム: http://localhost:8080/rest/items

### 動かないとき

- DataSource の JNDI が `jdbc/oracle` に解決されるか（`jboss-web.xml`）
- `ojdbc11` モジュールがサーバー起動後も残っているか（`module add` はインストール先の `modules/` にファイルを作る）
- 管理コンソールのランタイムでデプロイが `OK` か
- ログに `NameNotFoundException`（DataSource）や ORA-00942（テーブル未作成）が出ていないか

Liberty と EAP を同時に使う場合、ポートは Liberty が 9080、EAP が 8080 で分かれている。

## 主な設定ファイル

| ファイル | 役割 |
| --- | --- |
| `src/main/liberty/config/bootstrap.properties` | Oracle の URL / ユーザー / パスワード |
| `src/main/liberty/config/server.xml` | Open Liberty、DataSource `jdbc/oracle` |
| `ItemMapper.java` | MyBatis の CRUD SQL |
| `SqlSessionFactoryProducer.java` | JNDI DataSource から `SqlSessionFactory` を作る |
| `pom.xml` | `ojdbc11` と MyBatis のバージョン |
| `src/main/webapp/WEB-INF/web.xml` | `/rest/*` を Jakarta REST に割り当て |

## Docker

Starter 同梱の Docker 手順は [README.eclipse-starter.md](README.eclipse-starter.md) を参照。Oracle 接続先はコンテナから到達できるホストにする。
