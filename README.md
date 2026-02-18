To clone project and use it in Vscode:

    1. “ Git clone https://github.com/Junyuquick/CS102-Splendor-Project.git “

    2. “ git checkout main "

    3. " git config —global user.name junyuquick “

    4. " git config —global user.email junyu...@computing.smu.edu.sg “

    Note: replace the name n email w ur own, can be anything


To create feature branch:
    
    1. " git pull "

    2. " git checkout main "
    
    3. " git checkout -b <feature>-<your name> "

    4. " git add . && git commit -m "first commit "

    5. " git push -u origin <feature>-<your name> "

    Note: after "git pull", if there's any conflicts between 2ppl work, ask the chat
    Note: the name for feature branch is eg. config-junyu


To delete feature branch:

    1. " git checkout main "

    2. go to github and manually delete the remote branch with <feature>-<your name>

    3. " git fetch --prune origin "

    4. " git branch -d <feature>-<name> "

    Note: delete the feature branch once youre done with it, if we find bugs in the future, we can create a new feature branch for it, so no worries. this is to prevent buildup of too many dead branches


For subsequent uses on Vscode:

    1. Before you begin each coding session: "git pull"

    2. ensure you are on correct branch: "git checkout <feature>-<your name>"

    2. Once youre done, find the "COMMIT" button in Vscode, type the commit message and press it

    Note: for commit message, we can type "[filename]:[short exp of what u did] - [name]"
    • eg. "Player.java, Board.java: added attributes/methods - jun yu"
    • it’ll help us debug, and revert commits easily
    • we are not doing feature branch push as its a small project
    