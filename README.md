<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=F5F0E6&height=160&section=header&text=OFNI-MAKER&fontSize=48&fontColor=2D5016&animation=fadeIn&fontAlignY=38&desc=おふに・めーかー&descAlignY=18&descSize=16&descAlign=50" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Angular-19-C84B31?style=flat-square&logo=angular&logoColor=white" />
  <img src="https://img.shields.io/badge/Ionic-8-3880FF?style=flat-square&logo=ionic&logoColor=white" />
  <img src="https://img.shields.io/badge/Capacitor-6-119EFF?style=flat-square&logo=capacitor&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-2D5016?style=flat-square&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Java-25-9B8B7A?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-3.9-D4A574?style=flat-square&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/ONNX_Runtime-GPU-8B7355?style=flat-square&logo=nvidia&logoColor=white" />
  <img src="https://img.shields.io/badge/Ollama-Local-2D5016?style=flat-square&logo=ollama&logoColor=white" />
</p>

<p align="center">
  <b>Closet virtual · Local-first · IA sin nubes</b><br/>
  <sub>Tu guardarropa, inteligente. 100 % local, 100 % tuyo.</sub>
</p>

---

## Vision

**OFNI-Maker** nace de una idea simple: tu ropa merece un gestor inteligente que respete tu privacidad.

En un mundo donde cada foto que subes a una "nube" se vuelve producto, OFNI corre **todo localmente** — desde la clasificacion de prendas hasta la generacion de outfits. No enviamos tus datos a ningun servidor externo. Tu guardarropa vive en tu maquina, en tu telefono, bajo tu control.

El nombre viene del japones *ofuni* (おふに / 御服に) — "para tu vestimenta". Porque creemos que la tecnologia debe adaptarse a ti, no al reves.

---

## Features principales

- **Clasificacion automatica de prendas** — Subi una foto y el modelo IA identifica tipo, categoria, material y atributos visuales al instante.
- **Extraccion de paleta cromatica** — Cada prenda analizada genera sus colores HEX dominantes para combinar como un profesional.
- **Puntuacion termica** — Las prendas reciben un score de calor y cobertura (1-5) para saber que ponerte segun la temporada.
- **Outfits por clima** — Conecta con el clima de tu ciudad y recibe combinaciones armonicas generadas por IA local.
- **Offline-first** — Sin conexion? Sin problema. Todo funciona localmente con sincronizacion cuando vuelvas.
- **Multiplataforma** — Web, Android e iOS desde un mismo codigo base.

---

## Como surge

OFNI comenzo como un proyecto personal para resolver un problema cotidiano: tener un armario lleno de ropa y "no tener que ponerme". La clave no era comprar mas, sino **conocer mejor lo que ya tenia**.

Con un modelo de clasificacion de prendas ya entrenado (MobileNetV4 + ONNX), el siguiente paso es construir el backend que de vida a todo el sistema: APIs para gestionar el guardarropa, logica de outfits, conexion a base de datos y mas.

---

## Estado actual

| Modulo | Estado |
| :--- | :--- |
| Modelo de clasificacion IA | ✅ Listo (ver `ai-models/`) |
| Backend Spring Boot | 🚧 En desarrollo |
| Frontend Angular/Ionic | 🚧 En desarrollo |
| Base de datos / persistencia | ⏳ Pendiente |
| Docker / infraestructura local | 🚧 En progreso |

---

## Quick Start (para developers)

```bash
# 1. Clonar
git clone https://github.com/JkVely/Ofni-Maker.git
cd Ofni-Maker

# 2. Infraestructura IA local (Docker)
cp .env.example .env
docker compose up -d withoutbg ollama

# 3. Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -DskipTests

# 4. Frontend
cd frontend
npm install
ionic serve --port 8100
```

> Documentacion detallada del stack tecnico en la [Wiki](https://github.com/JkVely/Ofni-Maker/wiki).

---

## Paleta cromatica OFNI

| Token | Hex | Uso principal |
| :--- | :--- | :--- |
| `Washi` | `#F5F0E6` | Fondo general, papel japones |
| `Sumi` | `#2C2C2C` | Texto principal, tinta |
| `Moss` | `#2D5016` | Acentos primarios, botones confirmacion |
| `Akane` | `#C84B31` | Acentos secundarios, alertas suaves |
| `Stone` | `#9B8B7A` | Texto terciario, bordes, deshabilitado |
| `Wood` | `#D4A574` | Detalles estructurales, hover states |

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=2D5016&height=80&section=footer" />
</p>

<p align="center">
  <b>OFNI-Maker</b> · おふに・めーかー · <i>「少ないもので、豊かに」</i>
</p>