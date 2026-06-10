import os
import yaml
import torch
import torch.nn as nn
import torch.optim as optim
import timm # <--- AGREGAMOS TIMM AQUÍ
from tqdm import tqdm
from dataset_loader import get_dataloaders
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def load_config(config_path=None):
    if config_path is None:
        config_path = os.path.join(SCRIPT_DIR, "config.yaml")
    with open(config_path, "r") as f:
        return yaml.safe_load(f)

# LE PASAMOS EL NOMBRE DEL MODELO COMO PARÁMETRO
def create_model(model_name, num_classes):
    model = timm.create_model(model_name, pretrained=True, num_classes=num_classes)
    return model

def train(config_path=None):
    config = load_config(config_path)
    
    # Hiperparámetros
    data_path = config["dataset"]["path"]
    batch_size = config["dataset"]["batch_size"]
    num_workers = config["dataset"]["num_workers"]
    num_classes = config["model"]["num_classes"]
    model_name = config["model"]["name"]
    epochs = config["training"]["epochs"]
    lr = config["training"]["learning_rate"]
    weight_decay = config["training"]["weight_decay"]
    checkpoint_dir = config["training"]["checkpoint_dir"]
    
    os.makedirs(checkpoint_dir, exist_ok=True)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Entrenando en dispositivo: {device}")

    # Dataloaders
    train_loader, val_loader, class_names = get_dataloaders(data_path, batch_size, num_workers)
    print(f"Clases detectadas ({len(class_names)}): {class_names}")
    
    # Modelo
    model = create_model(model_name, len(class_names)).to(device)
    
    # Pérdida y Optimizador
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=weight_decay)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode='max', factor=0.5, patience=3, verbose=True)

    best_acc = 0.0

    for epoch in range(epochs):
        model.train()
        running_loss = 0.0
        
        # Loop de entrenamiento
        train_pbar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{epochs} [Train]")
        for inputs, labels in train_pbar:
            inputs, labels = inputs.to(device), labels.to(device)
            
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            
            running_loss += loss.item() * inputs.size(0)
            train_pbar.set_postfix({'loss': loss.item()})
            
        epoch_loss = running_loss / len(train_loader.dataset)
        
        # Validación
        model.eval()
        correct = 0
        total = 0
        val_loss = 0.0
        with torch.no_grad():
            val_pbar = tqdm(val_loader, desc=f"Epoch {epoch+1}/{epochs} [Val]")
            for inputs, labels in val_pbar:
                inputs, labels = inputs.to(device), labels.to(device)
                outputs = model(inputs)
                loss = criterion(outputs, labels)
                val_loss += loss.item() * inputs.size(0)
                
                _, predicted = torch.max(outputs.data, 1)
                total += labels.size(0)
                correct += (predicted == labels).sum().item()
        
        val_acc = 100 * correct / total
        val_epoch_loss = val_loss / len(val_loader.dataset)
        print(f"-> Train Loss: {epoch_loss:.4f} | Val Loss: {val_epoch_loss:.4f} | Val Acc: {val_acc:.2f}%")
        
        scheduler.step(val_acc)
        
        # Guardar mejor modelo
        if val_acc > best_acc:
            best_acc = val_acc
            save_path = os.path.join(checkpoint_dir, "best_mobilenetv3.pth")
            torch.save(model.state_dict(), save_path)
            print(f"   [!] Nuevo mejor modelo guardado en {save_path}")

    print(f"Entrenamiento finalizado. Mejor precisión en validación: {best_acc:.2f}%")

if __name__ == "__main__":
    train()
