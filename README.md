# gray-unicorn
Except site for 2018.

## Usage

### Setting up local environment
*Requirements:*
* MacOS 10.10.x
* Python 3.6 or higher

## Wagtail CMS setup
In a terminal window run:
```
pip3 install wagtail
wagtail start except
cd except
pip3 install -r requirements.txt
python3 manage.py migrate
python3 manage.py createsuperuser
python3 manage.py runserver
```
