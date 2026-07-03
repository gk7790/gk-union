# systemd deployment

This directory contains systemd templates for deploying `gk-admin` and `gk-api` on one Linux server.

## Layout

Recommended production paths:

```bash
/opt/gk-admin/app/gk-admin.jar
/opt/gk-admin/config/gk-admin.env
/opt/gk-api/app/gk-api.jar
/opt/gk-api/config/gk-api.env
/data/logs/gk-admin
/data/logs/gk-api
```

## First install

```bash
sudo useradd --system --create-home --home-dir /opt/gk --shell /usr/sbin/nologin gk
sudo mkdir -p /opt/gk-admin/app /opt/gk-admin/config /opt/gk-api/app /opt/gk-api/config
sudo mkdir -p /data/logs/gk-admin /data/logs/gk-api
sudo chown -R gk:gk /opt/gk-admin /opt/gk-api /data/logs/gk-admin /data/logs/gk-api

sudo cp deploy/systemd/gk-admin.service /etc/systemd/system/gk-admin.service
sudo cp deploy/systemd/gk-api.service /etc/systemd/system/gk-api.service
sudo cp deploy/env/gk-admin.env.example /opt/gk-admin/config/gk-admin.env
sudo cp deploy/env/gk-api.env.example /opt/gk-api/config/gk-api.env

sudo chmod 640 /opt/gk-admin/config/gk-admin.env /opt/gk-api/config/gk-api.env
sudo chown root:gk /opt/gk-admin/config/gk-admin.env /opt/gk-api/config/gk-api.env
```

Edit `/opt/gk-admin/config/gk-admin.env` and `/opt/gk-api/config/gk-api.env` before starting the services.

## Deploy jars

```bash
sudo cp gk-admin/target/gk-admin.jar /opt/gk-admin/app/gk-admin.jar
sudo cp gk-api/target/gk-api.jar /opt/gk-api/app/gk-api.jar
sudo chown gk:gk /opt/gk-admin/app/gk-admin.jar /opt/gk-api/app/gk-api.jar
```

## Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now gk-admin
sudo systemctl enable --now gk-api
```

## Common commands

```bash
sudo systemctl status gk-admin
sudo systemctl status gk-api

sudo journalctl -u gk-admin -f
sudo journalctl -u gk-api -f

sudo systemctl restart gk-admin
sudo systemctl restart gk-api
```

Application log files are written under `LOG_HOME`, for example `/data/logs/gk-admin` and `/data/logs/gk-api`.
