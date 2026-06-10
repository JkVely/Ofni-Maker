import pickle
import os
import json
import torch
from PIL import Image
from torchvision import transforms
from torch.utils.data import DataLoader, Dataset

# Las 13 categorías oficiales de DeepFashion2 (IDs del 1 al 13)
DEEPFASHION2_CLASSES = [
    "short sleeve top",      # 1 -> 0
    "long sleeve top",       # 2 -> 1
    "short sleeve outwear",  # 3 -> 2
    "long sleeve outwear",   # 4 -> 3
    "vest",                  # 5 -> 4
    "sling",                 # 6 -> 5
    "shorts",                # 7 -> 6
    "trousers",              # 8 -> 7
    "skirt",                 # 9 -> 8
    "short sleeve dress",    # 10 -> 9
    "long sleeve dress",     # 11 -> 10
    "vest dress",            # 12 -> 11
    "sling dress"            # 13 -> 12
]

class DeepFashion2Dataset(Dataset):
    def __init__(self, data_dir, transform=None):
        self.data_dir = data_dir
        self.image_dir = os.path.join(data_dir, 'image')
        self.anno_dir = os.path.join(data_dir, 'annos')
        self.transform = transform
        self.samples = []
        
        if not os.path.exists(self.image_dir) or not os.path.exists(self.anno_dir):
            raise FileNotFoundError(f"No se encontraron las carpetas 'image' y/o 'annos' en {data_dir}.")
            
        # --- INICIO DE LA OPTIMIZACIÓN (CACHÉ) ---
        cache_path = os.path.join(data_dir, 'annotations_cache.pkl')
        
        if os.path.exists(cache_path):
            # Si el caché ya existe, lo cargamos (¡esto toma 2 segundos!)
            print(f"Cargando caché de anotaciones desde {cache_path}... ¡Volando!")
            with open(cache_path, 'rb') as f:
                self.samples = pickle.load(f)
        else:
            # Si no existe, toca hacer el trabajo pesado
            print(f"Cargando anotaciones desde {self.anno_dir} (Esto tomará un rato, pero solo se hará una vez)...")
            for json_name in os.listdir(self.anno_dir):
                if not json_name.endswith('.json'):
                    continue
                    
                json_path = os.path.join(self.anno_dir, json_name)
                image_name = json_name.replace('.json', '.jpg')
                image_path = os.path.join(self.image_dir, image_name)
                
                if not os.path.exists(image_path):
                    continue
                    
                with open(json_path, 'r') as f:
                    anno_data = json.load(f)
                    
                for key, value in anno_data.items():
                    if key.startswith('item'):
                        category_id = value['category_id']
                        bbox = value['bounding_box']
                        
                        self.samples.append({
                            'image_path': image_path,
                            'bbox': bbox,
                            'label': category_id - 1
                        })
            
            # Al final del proceso, guardamos el resultado en el caché para el futuro
            print(f"Guardando caché en {cache_path} para la próxima ejecución...")
            with open(cache_path, 'wb') as f:
                pickle.dump(self.samples, f)

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        sample = self.samples[idx]
        
        # Cargar imagen
        image = Image.open(sample['image_path']).convert('RGB')
        
        # Recortar la prenda específica usando el Bounding Box
        xmin, ymin, xmax, ymax = sample['bbox']
        image = image.crop((xmin, ymin, xmax, ymax))
        
        if self.transform:
            image = self.transform(image)
            
        return image, sample['label']

def get_transforms(image_size=224):
    train_transform = transforms.Compose([
        transforms.Resize((256, 256)),
        transforms.RandomResizedCrop(image_size, scale=(0.8, 1.0)),
        transforms.RandomHorizontalFlip(),
        transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
    ])

    val_transform = transforms.Compose([
        transforms.Resize((256, 256)),
        transforms.CenterCrop(image_size),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
    ])
    
    return train_transform, val_transform

def get_dataloaders(data_dir, batch_size=32, num_workers=4, image_size=224):
    train_dir = os.path.join(data_dir, 'train')
    # En DeepFashion2 suele llamarse validation en vez de val
    val_dir = os.path.join(data_dir, 'validation')
    
    train_transform, val_transform = get_transforms(image_size)

    train_dataset = DeepFashion2Dataset(train_dir, transform=train_transform)
    val_dataset = DeepFashion2Dataset(val_dir, transform=val_transform)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=num_workers)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, num_workers=num_workers)

    return train_loader, val_loader, DEEPFASHION2_CLASSES
