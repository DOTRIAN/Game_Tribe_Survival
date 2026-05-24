# JavaFX Team Setup

Project nay da duoc cau hinh lai de dung Maven, khong can moi nguoi tu add JavaFX SDK thu cong trong IntelliJ nua.

## Yeu cau

- IntelliJ IDEA
- JDK 21 tro len
  - Khuyen nghi: Temurin 21 LTS
  - JDK 25 van dung duoc, vi project compile voi `--release 21`

## Cach mo project cho ca team

1. Clone repo.
2. Mo thu muc goc project `Game__TEST` trong IntelliJ.
3. IntelliJ se nhan ra [pom.xml](../pom.xml) va hien nut `Load Maven Project` hoac tu dong import.
4. Chon Project SDK la JDK 21 hoac JDK 25.
5. Cho IntelliJ tai dependency Maven xong.

Sau buoc nay, cac import `javafx.*` se duoc nhan ma khong can vao `Project Structure > Libraries` de add tay.

## Cach chay

### Cach 1: IntelliJ

- Mo file [Main.java](../src/main/Main.java)
- Run class `main.Main`

### Cach 2: Maven

Neu may co Maven:

```bash
mvn javafx:run
```

## Neu IntelliJ van bao do JavaFX

Lam lan luot:

1. Mo tab Maven va bam `Reload All Maven Projects`
2. Vao `File > Project Structure > Project`
   - Project SDK: JDK 21 hoac 25
   - Project language level: `SDK default` hoac `21`
3. `File > Invalidate Caches / Restart`

## Luu y quan trong

- Team khong nen commit cau hinh JavaFX SDK theo duong dan may ca nhan trong `.idea/libraries/...`
- Nguon phu thuoc chinh tu nay la Maven qua [pom.xml](../pom.xml)
- Game hien tai doc asset bang duong dan tu project root nhu `assets/...`, `data/...`, nen khi run hay mo project o dung thu muc goc repo
