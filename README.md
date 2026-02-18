To clone project and use it in Vscode:

    1. “ Git clone https://github.com/Junyuquick/CS102-Splendor-Project.git “

    2. “ git checkout main "

    3. " git config —global user.name junyuquick “

    4. " git config —global user.email junyu...@computing.smu.edu.sg “

    Note: replace the name n email w ur own, can be anything


To create feature branch:
    
    1. " git pull "

    2. " git checkout main "
    
    3. " git checkout -b feature/<feature name> "

    4. " git add . && git commit -m "first commit "

    5. " git push -u origin feature/<feature name> "

    Note: after "git pull", if there's any conflicts between 2ppl work, ask the chat
    Note: the name for feature branch is eg. feature/config


To delete feature branch:

    1. " git checkout main "

    2. go to github and manually delete the remote branch with feature/<feature name>

    3. " git fetch --prune origin "

    4. " git branch -d feature/<feature name> "

    Note: delete the feature branch once youre done with it, if we find bugs in the future, we can create a new feature branch for it, so no worries. this is to prevent buildup of too many dead branches


For subsequent uses on Vscode:

    1. Before you begin each coding session: " git pull "

    2. ensure you are on correct branch: " git checkout feature/<feature name> "

    2. Once youre done, find the "COMMIT" button in Vscode, type the commit message and press it

    Note: for commit message, we can type eg. "add token-bank and take 3 rule"
    • it’ll help us debug, and revert commits easily
    

How to test other's features before merging to main:

    1. " git checkout main "

    2. " git pull origin main "

    3. " git checkout feature/<the friend's feature name> "

    4. " git pull "

    5. " git merge main "

    6. run compile.sh and run.sh to test it

    7. reset the changes " git reset --hard origin/feature/<friend's feature name>"

    8. decide whether u want to merge the other's feature branch to main on github