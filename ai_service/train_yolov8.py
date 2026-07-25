"""
Microbial Colony YOLOv8 Deep Learning Training Pipeline
======================================================
Author: Senior Computer Vision and Deep Learning Engineer
System: Automated Microbial Colony Counting and CFU Analysis

This script provides a complete, automated pipeline to train a YOLOv8 object
detection model on microbial colony datasets (such as AGAR or custom labeled Petri dishes).

Prerequisites:
    pip install ultralytics opencv-python numpy

Dataset Layout Expected:
    You should label your plate images in YOLO format (each image has a corresponding
    .txt file containing annotations: <class_id> <x_center> <y_center> <width> <height>).
    Class ID is typically 0 (Colony).

Usage:
    python train_yolov8.py --data_dir /path/to/raw/dataset --epochs 100 --imgsz 640
"""

import os
import sys
import argparse
import shutil

def create_directory_structure(root_dir):
    """
    Creates standard Ultralytics YOLOv8 directory structure inside dataset root.
    """
    subdirs = [
        "datasets/colony/images/train",
        "datasets/colony/images/val",
        "datasets/colony/labels/train",
        "datasets/colony/labels/val"
    ]
    
    print("[Dataset Init] Creating structured directories for training...")
    for sd in subdirs:
        path = os.path.join(root_dir, sd)
        os.makedirs(path, exist_ok=True)
        print(f"  Created: {path}")

def generate_yaml_config(root_dir):
    """
    Generates dataset.yaml required by Ultralytics YOLOv8 trainer.
    """
    yaml_content = f"""# Microbial Colony Dataset Configuration
path: {os.path.abspath(os.path.join(root_dir, 'datasets/colony'))}
train: images/train
val: images/val

# Classes
names:
  0: colony
"""
    yaml_path = os.path.join(root_dir, "datasets/colony/dataset.yaml")
    with open(yaml_path, "w") as f:
        f.write(yaml_content.strip())
    print(f"[Dataset Config] Generated YOLOv8 dataset config: {yaml_path}")
    return yaml_path

def train_model(yaml_config_path, epochs=100, batch_size=16, imgsz=640):
    """
    Imports Ultralytics and launches the YOLOv8 training loop.
    """
    try:
        from ultralytics import YOLO
    except ImportError:
        print("[Error] The 'ultralytics' library is required for deep learning training.")
        print("Please install it via: pip install ultralytics")
        sys.exit(1)
        
    print("\n" + "="*60)
    print("        LAUNCHING MICROBIAL COLONY TRAINING LOOP")
    print("="*60)
    print(f"Config YAML: {yaml_config_path}")
    print(f"Epochs:      {epochs}")
    print(f"Batch Size:  {batch_size}")
    print(f"Image Size:  {imgsz}x{imgsz}")
    print("="*60 + "\n")
    
    # 1. Load a pre-trained nano YOLOv8 model (great for tiny object/colony counting)
    model = YOLO("yolov8n.pt")
    
    # 2. Train on the custom dataset
    # hyper-parameters optimized for small colony detections:
    results = model.train(
        data=yaml_config_path,
        epochs=epochs,
        batch=batch_size,
        imgsz=imgsz,
        workers=4,
        device=0,           # Set to 'cpu' if no CUDA GPU is available
        patience=20,         # Early stopping
        save=True,           # Save checkpoints
        pretrained=True,     # Start from COCO weights
        optimizer="AdamW",   # Highly recommended for small/dense objects
        lr0=0.01,            # Initial learning rate
        box=7.5,             # Box loss weight
        cls=0.5,             # Class loss weight
        dfl=1.5,             # Distribution focal loss weight
        augment=True,        # Enable heavy augmentations (mosaic, rotation, scaling)
        val=True             # Run validation at the end of each epoch
    )
    
    print("\n" + "="*60)
    print("           TRAINING PIPELINE COMPLETE!")
    print("="*60)
    
    # Locate the best weights
    best_weights_path = os.path.join("runs", "detect", "train", "weights", "best.pt")
    if os.path.exists(best_weights_path):
        print(f"Success! Best model weights saved to: {best_weights_path}")
        # Copy to the local directory for immediate web integration
        local_target = os.path.join(os.path.dirname(__file__), "yolov8n-colony.pt")
        shutil.copy(best_weights_path, local_target)
        print(f"Copied weights directly to local service runner: {local_target}")
    else:
        print("Training finished. Check the 'runs/detect/train' folder for output weights.")
    print("="*60 + "\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Microbial Colony YOLOv8 Deep Learning Trainer")
    parser.add_argument("--root_dir", type=str, default=".", help="Root directory for dataset structuring")
    parser.add_argument("--epochs", type=int, default=100, help="Number of training epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--imgsz", type=int, default=640, help="Input image size")
    
    args = parser.parse_args()
    
    create_directory_structure(args.root_dir)
    yaml_config = generate_yaml_config(args.root_dir)
    
    print("\n[User Guidance] Directory structure is ready!")
    print("To train:")
    print("  1. Copy your labeled images (*.jpg) to: datasets/colony/images/train (and validation images to images/val)")
    print("  2. Copy corresponding YOLO label files (*.txt) to: datasets/colony/labels/train (and val)")
    print("  3. Run this script again to begin training.")
    
    # Run the training loop if dataset contains images, otherwise show warning
    train_img_dir = os.path.join(args.root_dir, "datasets/colony/images/train")
    if len(os.listdir(train_img_dir)) > 0:
        train_model(yaml_config, epochs=args.epochs, batch_size=args.batch, imgsz=args.imgsz)
    else:
        print("\n[Warning] Training deferred: No images detected in datasets/colony/images/train.")
        print("Please load your dataset and run this script to start deep learning training.")
