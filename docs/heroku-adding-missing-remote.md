This is how to resolve the following command line error message when pushing your app to Heroku:

~/Desktop/appname git push heroku master
fatal: 'heroku' does not appear to be a git repository
fatal: Could not read from remote repository.
fatal: 'heroku' does not appear to be a git repository
Please make sure you have the correct access rights and the repository exists.

The above error message appears because there's no remote named heroku. When you do a heroku create, if the git remote doesn't already exist, Heroku automatically creates one assuming you're in a git repo.

You can see your remotes by typing git remote -v. For my app app I see the following when checking which remote's exist:

~/Desktop/appname git remote -v
origin https://github.com/username/appname.git (fetch)
origin https://github.com/username/appname.git (push)

If you see a remote for your app, you can git push master and replace with the actual remote name.

$ git remote -v
heroku git@heroku.com:appname.git (fetch)
heroku git@heroku.com:appname.git (push)

If the remote's missing, like it was for me, you can add the remote with the following command:

git remote add heroku git@heroku.com:appname.git
