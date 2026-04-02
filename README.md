## How to play Single Player mode
1. On your machine, launch the game
   ```
   bash compile.sh
   bash run.sh
   ```
2. Select "Single Player", key in your name, and select the number of computer u want to play with



## How to play Multiplayer mode on same laptop
1. On your machine, launch the game
   ```
   bash compile.sh
   bash run.sh
   ```
2. Select "Multiplayer", Select "Multiplayer on Same Laptop"
3. Then key in your name, and select the number of computer u want to play with



## How to play Multiplayer mode on same laptop
1. to host the game on one laptop(host), run the server:
   ```
   bash compile.sh
   bash run.sh
   ```
2. Select "Multiplayer", Select "Multiplayer over Network"
3. Key in the your private IP address, any port number of the default(12345), and your name

   note:
   - you should use "ifconfig" to get your private IP address(usually starts with 10, 172 or 192)
   - tip: use ctrl f and type "en0", your private ip is in that block of information
   - you may choose to use your loopback address if u want though
   
   Default port is 12345.

4. The server will wait for players(clients) to join.
5. The server creator joins as the host and can press `Start Game` once at least 2/3/4 players(depends on config.properties) are in the lobby.
6. On each client machine, run the client:
   ```
   bash compile.sh
   bash run.sh
   ```
7. Enter the host IP address, port (default 12345), and your player name.
8. Wait in the lobby for the host to start the game.
