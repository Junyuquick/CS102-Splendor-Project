# run this to start the multiplayer server

javac -d classes $(find src -name '*.java') && java -cp classes network.GameServer