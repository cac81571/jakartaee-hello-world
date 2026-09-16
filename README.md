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

2. テーブルを用意する。アプリ起動時に EclipseLink が無いテーブルを作ることもあるが、手動なら `src/main/sql/items.sql` を実行する。

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

このアプリは **Entity・ネイティブ SQL・画面** が同じ列を共有している。DB だけ変えても動かない。

いま紐づいている場所:

| 場所 | 内容 |
| --- | --- |
| `src/main/sql/items.sql` | DDL の正 |
| `Item.java` | JPA エンティティ（SQL 結果のマッピング） |
| `ItemService.java` | CRUD の SQL と実行 |
| `src/main/webapp/index.html` | 入力欄と JSON |
| `ItemResource.java` | 必須チェック（いまは `id` と `name`） |

`persistence.xml` の `eclipselink.ddl-generation=create-or-extend-tables` は、**無いテーブルや足りない列を足す程度**。列の削除、型変更、主キー方針の変更はしない。開発中は SQL で明示するのが確実。

### 列を足す例（`STATUS`）

1. サーバーを止める（`Ctrl+C` または `.\mvnw.cmd liberty:stop`）。
2. Oracle で ALTER する（既存データを残す場合）。

```sql
ALTER TABLE ITEMS ADD STATUS VARCHAR2(20);
```

3. `src/main/sql/items.sql` の `CREATE TABLE` にも同じ列を足す（次から新規作成するとき用）。
4. `Item.java` にフィールドと getter / setter を足す。`@Column(name = "STATUS")` を付ける。
5. `ItemService.java` の SELECT / INSERT / UPDATE に列を足す。`?1` から始まるプレースホルダ番号を `setParameter` と揃える。

6. 画面の入力・一覧・`payload()` に `status` を足す。API の JSON も同じプロパティ名になる。
7. サーバーを再起動する。`.\mvnw.cmd liberty:run`

### 列を消す・型を変える・作り直す

ALTER で落とすか、開発用ならドロップして作り直す。

```sql
DROP TABLE ITEMS;
```

そのあと `src/main/sql/items.sql` を実行するか、アプリ起動時の DDL 生成に任せる。Entity と `ItemService` の SQL から消した列を抜かないと、INSERT / UPDATE が失敗する。

### テーブル名を変える

`ITEMS` は次に直書きされている。

- `Item.java` の `@Table(name = "...")`
- `ItemService.java` の SQL
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

CRUD の SQL は `ItemService` に直書きし、`EntityManager#createNativeQuery` で実行する。

```java
SELECT ID, NAME, DESCRIPTION FROM ITEMS ORDER BY ID
SELECT ID, NAME, DESCRIPTION FROM ITEMS WHERE ID = ?1
INSERT INTO ITEMS (ID, NAME, DESCRIPTION) VALUES (?1, ?2, ?3)
UPDATE ITEMS SET NAME = ?1, DESCRIPTION = ?2 WHERE ID = ?3
DELETE FROM ITEMS WHERE ID = ?1
```

プレースホルダは JPA の `?1`, `?2` …。テーブルや列を変えたらこの SQL と `Item.java`、`setParameter` の番号を揃える。

## 主な設定ファイル

| ファイル | 役割 |
| --- | --- |
| `src/main/liberty/config/bootstrap.properties` | Oracle の URL / ユーザー / パスワード |
| `src/main/liberty/config/server.xml` | Open Liberty、DataSource `jdbc/oracle` |
| `src/main/resources/META-INF/persistence.xml` | JPA（`oraclePU`） |
| `pom.xml` | `ojdbc11` のバージョン。起動時に `jdbc/ojdbc11.jar` へコピー |
| `src/main/webapp/WEB-INF/web.xml` | `/rest/*` を Jakarta REST に割り当て |

## Docker

Starter 同梱の Docker 手順は [README.eclipse-starter.md](README.eclipse-starter.md) を参照。Oracle 接続先はコンテナから到達できるホストにする。
