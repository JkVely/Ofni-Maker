# ai-models

Entrenamiento de clasificador de ropa con MobileNetV3 usando DeepFashion2 y exportación a ONNX.

## Requerimientos

- Python 3.10 o superior
- uv instalado

Este proyecto usa `uv` y resuelve `torch` y `torchvision` desde el índice de PyTorch definido en `pyproject.toml`.

## Instalación (uv)

Ejecuta desde la raíz de `ai-models`:

```bash
uv sync
```

Para correr scripts, usa `uv run` para asegurar que se use el entorno correcto:

```bash
uv run python --version
```

## Dataset esperado

El clasificador espera esta estructura exacta:

```text
data/deepfashion2/
	train/
		image/
		annos/
	validation/
		image/
		annos/
```

Reglas importantes:

- Extrae los ZIP de `train` y `validation` para que queden como carpetas.
- Cada JSON en `annos` debe mapear a una imagen JPG con el mismo nombre base en `image`.
- El loader toma cada `item*` del JSON como una muestra independiente, hace crop por `bounding_box` y usa `category_id - 1` como etiqueta.
- Carpetas de metadata como `json_for_test` o `json_for_validation` no participan en este entrenamiento.

## Como funciona clothing_classifier

Archivos principales:

- `training/clothing_classifier/dataset_loader.py`
	- Define las 13 clases oficiales de DeepFashion2.
	- Crea `DataLoader` para train y validation.
	- Aplica augmentations en train y transformaciones deterministas en validation.

- `training/clothing_classifier/train_mobilenetv3.py`
	- Carga configuración desde `training/clothing_classifier/config.yaml`.
	- Construye `mobilenet_v3_small` preentrenado y reemplaza la capa final con 13 clases.
	- Entrena con `AdamW` y `CrossEntropyLoss`.
	- Usa `ReduceLROnPlateau` según accuracy de validación.
	- Guarda el mejor checkpoint en `checkpoints/best_mobilenetv3.pth`.

- `training/clothing_classifier/config.yaml`
	- Hiperparámetros de dataset, modelo y entrenamiento.

## Entrenamiento

Desde la raíz de `ai-models`:

```bash
uv run python training/clothing_classifier/train_mobilenetv3.py
```

Salida principal:

```text
checkpoints/best_mobilenetv3.pth
```

## Exportación a ONNX

Una vez entrenado, exporta el modelo así:

```bash
uv run python training/export_to_onnx.py
```

Salida esperada:

```text
training/mobilenetv3_clothing.onnx
```
