const fs = require('fs');
const path = require('path');

// Base64 representation of a 100x100 dark charcoal solid color PNG
// Matches dark visual contrast required by circular lab lighting systems
const MOCK_PLATE_BASE64 = 'iVBORw0KGgoAAAANSUhEUgAAAGQAAABkAQMAAABKL5n3AAAABlBMVEUeHh4eHh5vD18gAAAAAnRSTlMAAQGU/XsAAAARSURBVDjNY2AYBaNgFIwCCwAABQAAAeV4/8cAAAAASUVORK5CYII=';

function seedMockImages() {
  const uploadDir = path.join(__dirname, '../uploads');
  if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
  }

  const files = ['sample-001.png', 'sample-002.png', 'sample-003.png'];
  
  files.forEach(filename => {
    const filePath = path.join(uploadDir, filename);
    if (!fs.existsSync(filePath)) {
      try {
        const buffer = Buffer.from(MOCK_PLATE_BASE64, 'base64');
        fs.writeFileSync(filePath, buffer);
        console.log(`[Image Seeder] Created mock plate image: ${filename}`);
      } catch (error) {
        console.error(`[Image Seeder] Error seeding ${filename}:`, error.message);
      }
    }
  });
}

module.exports = { seedMockImages };
