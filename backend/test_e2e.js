const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');

async function upload(imageName) {
  const form = new FormData();
  form.append('sampleImage', fs.createReadStream(imageName));
  form.append('batchId', 'BATCH-' + imageName);
  form.append('applianceType', 'Catheter');
  form.append('dilutionFactor', '1');
  form.append('operatorName', 'E2E Tester');
  form.append('comments', 'Test comment');

  try {
    const response = await axios.post('http://localhost:5000/api/samples/upload', form, {
      headers: {
        ...form.getHeaders(),
        'X-Request-Id': 'req-' + imageName,
        'X-Requested-With': 'XMLHttpRequest'
      }
    });
    console.log('[SUCCESS] ' + imageName + ' -> Sample ID:', response.data.sample.sampleId);
  } catch (err) {
    console.error('[ERROR] ' + imageName + ' ->', err.response ? err.response.data : err.message);
  }
}

async function run() {
  console.log('Uploading Image A...');
  await upload('image_a.png');
  console.log('Uploading Image B...');
  await upload('image_b.png');
  console.log('Uploading Image C...');
  await upload('image_c.png');
}
run();
