# gray-unicorn
Except site for 2018.

## Usage

### Requirements
* MacOS 10.10.x
* GIT
* Python 3.6
* Node v8.11.x
* NPM 5.6.x

## Run Wagtail on your local machine
First, from a terminal window, clone this repository somewhere on your hard drive.  
```
git clone git@github.com:heidilaw4/gray-unicorn.git
```
Then navigate to the project root and run:
```
pip install -r requirements.txt
python manage.py migrate
python3 manage.py runserver
```
For viewing the website, navigate to:
```
http://localhost:8000
```

For the admin login page, navigate to:
```
http://localhost:8000/admin
```
Default credentials are `admin:exceptdev123`

## Run SASS watcher & builder
In a terminal window navigate to the project root, and run:
```
python3 manage.py bulma start
```
You'll find the rebuilt CSS file in `except/static/bulma/css/style.css`
