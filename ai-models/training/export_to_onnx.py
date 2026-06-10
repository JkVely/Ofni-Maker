import os
import torch
import yaml
import onnx
import onnxsim
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))

sys.path.append(os.path.join(SCRIPT_DIR, "clothing_classifier"))
from trainer import create_model

def load_config(config_path=None):
    if config_path is None:
        config_path = os.path.join(SCRIPT_DIR, "clothing_classifier", "config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)


def resolve_path(base_dir, path_value):
    if os.path.isabs(path_value):
        return path_value
    return os.path.abspath(os.path.join(base_dir, path_value))

def export_model():
    config = load_config()
    num_classes = config["model"]["num_classes"]
    model_name = config["model"]["name"]
    
    checkpoint_dir = resolve_path(PROJECT_ROOT, config["training"]["checkpoint_dir"])
    weights_path = os.path.join(checkpoint_dir, "best_mobilenetv3.pth")
    image_size = config["export"]["image_size"]
    opset = config["export"]["onnx_opset"]
    output_path = os.path.join(SCRIPT_DIR, "mobilenetv4_clothing.onnx")

    if not os.path.exists(weights_path):
        raise FileNotFoundError(f"No se encontraron pesos en {weights_path}. Entrena el modelo primero.")

    print(f"Cargando modelo {model_name} con {num_classes} clases...")
    print("Exportando en CPU para maximizar compatibilidad del ONNX.")

    model = create_model(model_name, num_classes) 
    model.load_state_dict(torch.load(weights_path, map_location="cpu", weights_only=True))
    model.eval()

    # Crear input tensor (batch=1, channels=3, H, W)
    dummy_input = torch.randn(1, 3, image_size, image_size)

    print(f"Exportando a ONNX ({output_path})...")
    torch.onnx.export(
        model, 
        dummy_input, 
        output_path,
        export_params=True,
        opset_version=opset,
        do_constant_folding=True,
        input_names=['input'],
        output_names=['output'],
        dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
    )

    print("Simplificando el modelo con onnxsim...")
    onnx_model = onnx.load(output_path)
    model_simp, check = onnxsim.simplify(onnx_model)
    
    if check:
        onnx.save(model_simp, output_path)
        print(f"¡Modelo simplificado y guardado en {output_path}!")
        print("Este archivo .onnx es el que debes copiar a tu carpeta 'backend/src/main/resources/models/'")
    else:
        print("Advertencia: No se pudo verificar el modelo simplificado.")

if __name__ == "__main__":
    export_model()
