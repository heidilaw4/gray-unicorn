FROM python:3.7
LABEL maintainer="hello@wagtail.io"

ENV PYTHONUNBUFFERED 1
ENV DJANGO_ENV dev

COPY ./requirements.txt /code/requirements.txt
RUN pip3 install -r /code/requirements.txt
RUN pip3 install gunicorn

COPY . /code/
WORKDIR /code/

CD except
RUN python3 manage.py migrate

RUN useradd wagtail
RUN chown -R wagtail /code
USER wagtail

EXPOSE 8000
CMD exec gunicorn except.wsgi:application --bind 0.0.0.0:8000 --workers 3
