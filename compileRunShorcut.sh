# javac -d classes -cp "src" src/Main.java && java -cp "classes" Main

javac $(find src -name '*.java') && java -cp src SwingMain