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
  <sub>Clasificacion automatica de prendas, paletas cromaticas, puntuacion termica y generacion de outfits por clima.</sub>
</p>

---

## Features

- **Upload inteligente** — Fotografia una prenda, se quita el fondo automaticamente y se clasifica por tipo, categoria y material.
- **Inferencia local GPU** — MobileNetV4 via ONNX Runtime Java corre en tu NVIDIA RTX (~20 ms por imagen).
- **Extraccion de colores** — K-Means sobre OpenCV genera una paleta HEX dominante de cada prenda.
- **Puntuacion termica** — Cada prenda recibe score de calor (1-5) y cobertura (1-5) para filtrar por temporada.
- **Outfits por clima** — Lee la temperatura actual de tu ciudad via Open-Meteo y genera combinaciones armonicas con Ollama.
- **100 % offline-capable** — PWA + SQLite local en Capacitor. Tu guardarropa nunca sale de tu red.
- **Multiplataforma** — Un codigo Angular sirve PWA web, Android APK e iOS nativo.

---

## Instalacion rapida

### Requisitos

| Componente | Version minima | Nota |
| :--- | :--- | :--- |
| OS | Linux, Windows 11, macOS | Linux/Arch recomendado |
| GPU | NVIDIA 8 GB VRAM | RTX 4060 Ti testeado |
| RAM | 16 GB | 32 GB recomendado |
| Java | OpenJDK 25 | Virtual threads + records nativos |
| Maven | 3.9.x | `choco install maven` / `pacman -S maven` |
| Node.js | 22 LTS | `nvm install 22` |
| Docker | 25.x + Compose + NVIDIA Container Toolkit | Para withoutbg y Ollama |
| Python / uv | 3.12 + uv 0.4+ | Solo para entrenamiento/export ONNX |

### 1. Repositorio

```bash
git clone https://github.com/JkVely/Ofni-Maker.git
cd Ofni-Maker
```

### 2. Infraestructura Docker (IA local)

```bash
cp .env.example .env
docker compose up -d withoutbg ollama

# Descargar modelos LLM (primera vez)
docker exec ofni-maker-ollama-1 ollama pull qwen2.5-vl:7b
docker exec ofni-maker-ollama-1 ollama pull mistral-small:24b
```

### 3. Backend (Spring Boot + Maven)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -DskipTests

# En Windows: mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -DskipTests
```

Disponible en `http://localhost:8080`.

### 4. Frontend (Angular + Ionic)

```bash
cd frontend
npm install
ionic serve --port 8100
```

Disponible en `http://localhost:8100`.

### 5. Compilar app movil nativa

```bash
# Android
ionic cap sync android
ionic cap open android

# iOS (requiere macOS + Xcode)
ionic cap sync ios
ionic cap open ios
```

---

## Estructura del proyecto

```text
ofni-maker/
├── ai-models/ # Python: entrenamiento y export a ONNX
│ ├── training/
│ │ ├── clothing_classifier/
│ │ │ ├── train_mobilenetv4.py
│ │ │ └── export_to_onnx.py
│ ├── notebooks/
│ ├── data/ # (gitignored)
│ ├── checkpoints/ # (gitignored)
│ ├── pyproject.toml # uv: torch, timm, opencv-python
│ └── uv.lock
│
├── backend/ # Spring Boot · Java 25 · Maven
│ ├── src/main/java/com/ofni/
│ │ ├── config/ # Beans: CORS, ONNX, Ollama, OpenMeteo
│ │ ├── controller/ # REST: Clothing, Outfit, Weather
│ │ ├── service/ # Logica de negocio
│ │ ├── model/ # JPA Entities + Enums
│ │ ├── repository/ # Spring Data JPA
│ │ ├── dto/ # Records inmutables (Java 25)
│ │ ├── inference/ # ONNX Runtime + preprocess
│ │ └── exception/ # GlobalExceptionHandler
│ ├── src/main/resources/
│ │ ├── application.yml
│ │ └── models/
│ │ └── *.onnx # Modelo exportado (no commitear pesados)
│ ├── src/test/
│ ├── pom.xml
│ ├── Dockerfile
│ └── target/ # Maven build (gitignored)
│
├── frontend/ # Angular 19 · Ionic 8 · Capacitor 6
│ ├── src/app/
│ │ ├── components/ # Presentacionales: card, pill, app-bar
│ │ ├── screens/ # Smart containers: home, wardrobe, upload
│ │ ├── services/ # HTTP clients
│ │ ├── stores/ # Zustand state
│ │ └── types/ # Interfaces TypeScript
│ ├── android/ # Generado por Capacitor
│ ├── ios/ # Generado por Capacitor
│ ├── angular.json
│ ├── capacitor.config.ts
│ ├── tailwind.config.ts
│ └── package.json
│
├── uploads/ # Imagenes local (gitignored)
│ ├── original/
│ └── processed/
│
├── docker-compose.yml
├── .env.example
├── schema.json
├── .gitignore
├── LICENSE
└── README.md
```

---

## Stack

| Capa | Tecnologia | Rol |
| :--- | :--- | :--- |
| **Frontend** | Angular 19 + TypeScript 5.6 | UI declarativa, componentes reutilizables |
| | Ionic 8 | Navegacion movil nativa, overlays, transiciones |
| | Capacitor 6 | Bridge a camara, filesystem, SQLite nativo |
| | Tailwind CSS 4 | Estilos utility-first con paleta OFNI custom |
| | TanStack Query 5 | Cacheo server-state, reintentos, sincronizacion offline |
| | Zustand 4 | Estado global ligero (guardarropa actual, filtros) |
| **Backend** | Spring Boot 4.0 + Java 25 | API REST, virtual threads, records, pattern matching |
| | Maven 3.9 | Build, dependencias, packaging |
| | Spring AI 1.0.0-M6 | Abstraccion LLM local (OllamaChatClient) |
| | Spring Data JPA | Persistencia H2 / SQLite sin config externa |
| | ONNX Runtime Java GPU 1.19 | Inferencia MobileNetV4 en CUDA dentro de la JVM |
| | JavaCV / OpenCV 1.5.10 | K-Means clustering para extraccion de colores |
| **IA Local** | Ollama latest | Motor inferencia LLM + Vision 100% local |
| | withoutbg | Docker self-hosted, quita fondo via API REST |
| | ComfyUI *(Fase 3)* | Stable Diffusion + IP-Adapter para virtual try-on |
| **ML** | Python 3.12 + uv | Entorno reproducible, gestion de deps |
| | timm + PyTorch 2.4 | Fine-tuning MobileNetV4, export ONNX |
| **Infra** | Docker + Compose + NVIDIA Toolkit | Orquestacion de servicios con passthrough GPU |

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