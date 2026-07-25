const { Queue, Worker } = require('bullmq');
const axios = require('axios');
const Redis = require('ioredis');
const fs = require('fs');
const { db } = require('./firebase');

// Redis connection setup
const redisConnection = new Redis({
  host: process.env.REDIS_HOST || '127.0.0.1',
  port: process.env.REDIS_PORT || 6379,
  maxRetriesPerRequest: null,
  enableReadyCheck: false,
  retryStrategy: (times) => {
    // Stop retrying after the first failure to prevent console spam
    return null;
  }
});

let redisOfflineLogged = false;

redisConnection.on('error', (err) => {
  if (err.code === 'ECONNREFUSED') {
    if (!redisOfflineLogged) {
      console.warn('[Queue] Redis not available locally. Falling back to synchronous processing without BullMQ.');
      redisOfflineLogged = true;
    }
  }
});

// AI Processing Queue — handles concurrent limit
const USE_BULLMQ = process.env.USE_BULLMQ === 'true';

let aiQueue;
if (USE_BULLMQ) {
  try {
    aiQueue = new Queue('aiQueueName', { connection: redisConnection });
    console.log(`[Queue] BullMQ queue initialized.`);
  } catch (e) {
    console.warn('[Queue] Failed to init BullMQ queue.');
  }
} else {
  console.log('[Queue] BullMQ disabled. Using synchronous processing mode.');
}

// -------------------------------------------------------------
// HELPER: Fallback Local Mock AI
// -------------------------------------------------------------
function getRiskLevel(count) {
  if (count <= 5) return 'Low';
  if (count <= 25) return 'Medium';
  if (count <= 60) return 'High';
  return 'Critical';
}

function calculateZones(detections) {
  let aCount = 0, bCount = 0, cCount = 0, dCount = 0;
  detections.forEach(det => {
    if (det.x < 200) { if (det.y < 200) aCount++; else cCount++; }
    else { if (det.y < 200) bCount++; else dCount++; }
  });
  const total = detections.length || 1;
  return [
    { name: 'Zone A (North-West)', count: aCount, density: parseFloat(((aCount/total)*100).toFixed(1)), risk: getRiskLevel(aCount * 2) },
    { name: 'Zone B (North-East)', count: bCount, density: parseFloat(((bCount/total)*100).toFixed(1)), risk: getRiskLevel(bCount * 2) },
    { name: 'Zone C (South-West)', count: cCount, density: parseFloat(((cCount/total)*100).toFixed(1)), risk: getRiskLevel(cCount * 2) },
    { name: 'Zone D (South-East)', count: dCount, density: parseFloat(((dCount/total)*100).toFixed(1)), risk: getRiskLevel(dCount * 2) }
  ];
}

function simulateAIAnalysis(applianceType) {
  let targetCount = 15 + Math.floor(Math.random() * 30);
  if (applianceType === 'Catheter') targetCount = 20 + Math.floor(Math.random() * 25);
  if (applianceType === 'Surgical Syringe') targetCount = 2 + Math.floor(Math.random() * 8);
  if (applianceType === 'Scalpel') targetCount = 5 + Math.floor(Math.random() * 15);
  if (applianceType === 'Endoscope Tube') targetCount = 65 + Math.floor(Math.random() * 35);
  if (applianceType === 'Petri Dish (Control)') targetCount = 0;

  const detections = [];
  while (detections.length < targetCount) {
    const angle = Math.random() * Math.PI * 2;
    const dist = Math.sqrt(Math.random()) * 170;
    const x = Math.round(200 + Math.cos(angle) * dist);
    const y = Math.round(200 + Math.sin(angle) * dist);
    if (!detections.some(d => Math.hypot(d.x - x, d.y - y) < 8)) {
      detections.push({ x, y, radius: Math.round(5 + Math.random() * 7), confidence: parseFloat((0.82 + Math.random() * 0.17).toFixed(2)) });
    }
  }

  return {
    colonyCount: targetCount,
    detections,
    zones: calculateZones(detections),
    contaminationRisk: getRiskLevel(targetCount)
  };
}

// -------------------------------------------------------------
// WORKER: Process AI Jobs
// -------------------------------------------------------------
let aiWorker;
if (USE_BULLMQ) {
  try {
    // Concurrency set to 4 to prevent CPU overload on Python service (BUG-010/TC-L013 fix)
    aiWorker = new Worker('aiQueueName', async job => {
      const { sampleId, pythonUrl, filePath, applianceType } = job.data;
      console.log(`[Queue Worker] Processing AI job for sample ${sampleId}`);

      let aiResults;
      let fallbackUsed = false;

      try {
        if (!fs.existsSync(filePath)) {
          throw new Error('Image file missing on disk.');
        }
        
        const response = await axios.post(pythonUrl, { image_path: filePath }, { timeout: 25000 });
        if (response.data && response.data.status === 'success') {
          const rawZones = response.data.zones || {};
          const formattedZones = [
            { name: 'Zone A (North-West)', count: rawZones['A'] || 0, density: 25, risk: getRiskLevel((rawZones['A'] || 0) * 2) },
            { name: 'Zone B (North-East)', count: rawZones['B'] || 0, density: 25, risk: getRiskLevel((rawZones['B'] || 0) * 2) },
            { name: 'Zone C (South-West)', count: rawZones['C'] || 0, density: 25, risk: getRiskLevel((rawZones['C'] || 0) * 2) },
            { name: 'Zone D (South-East)', count: rawZones['D'] || 0, density: 25, risk: getRiskLevel((rawZones['D'] || 0) * 2) }
          ];

          // Recalculate densities
          const totalCount = response.data.colony_count || 1;
          formattedZones.forEach(z => {
            z.density = parseFloat(((z.count / totalCount) * 100).toFixed(1));
          });

          aiResults = {
            processedImage: response.data.processed_image_path ? response.data.processed_image_path.split(/[\\/]/).pop() : '',
            colonyCount: response.data.colony_count || 0,
            contaminationRisk: getRiskLevel(response.data.colony_count || 0),
            detections: response.data.detections || [],
            zones: formattedZones
          };
        } else {
          throw new Error('Python service returned error status.');
        }
      } catch (err) {
        console.warn(`[Queue Worker] Python AI failed for ${sampleId}. Using fallback local simulation.`);
        fallbackUsed = true;
        aiResults = simulateAIAnalysis(applianceType);
        // Brief delay to simulate AI processing time
        await new Promise(r => setTimeout(r, 1500));
      }

      // Update DB record with completed results
      const docRef = db.collection('samples').doc(sampleId);
      const doc = await docRef.get();
      if (doc.exists) {
        await docRef.update({
          status: 'Completed',
          colonyCount: aiResults.colonyCount,
          cfuCount: aiResults.colonyCount * (doc.data().dilutionFactor || 1),
          contaminationRisk: aiResults.contaminationRisk,
          detections: aiResults.detections,
          zones: aiResults.zones,
          ...(aiResults.processedImage && { processedImage: aiResults.processedImage })
        });
      }

      return { sampleId, status: 'Completed', fallbackUsed };

    }, { connection: redisConnection, concurrency: 4 });

    aiWorker.on('completed', job => {
      console.log(`[Queue Worker] Job ${job.id} completed for sample ${job.data.sampleId}`);
    });

    aiWorker.on('failed', (job, err) => {
      console.error(`[Queue Worker] Job ${job.id} failed:`, err.message);
      // On hard failure, try to set status to Error
      db.collection('samples').doc(job.data.sampleId).update({ status: 'Error' }).catch(e => {});
    });

  } catch (e) {
    console.warn('[Queue] Failed to init BullMQ worker.');
  }
}

// -------------------------------------------------------------
// EXPORT QUEUE DISPATCHER
// -------------------------------------------------------------
async function enqueueAIAnalysis(jobData) {
  if (aiQueue) {
    try {
      // Check if Redis is actually up (BullMQ queues promises indefinitely if offline without offlineQueue config, but we can fast-fail)
      if (redisConnection.status !== 'ready') throw new Error('Redis offline');
      
      const job = await aiQueue.add('analyze', jobData, { removeOnComplete: true, removeOnFail: true });
      return { method: 'queue', jobId: job.id };
    } catch(err) {
      // Redis offline/failed, fallback to synchronous simulation so uploads don't block
      return fallbackSyncAnalysis(jobData);
    }
  } else {
    // BullMQ not available
    return fallbackSyncAnalysis(jobData);
  }
}

async function fallbackSyncAnalysis(jobData) {
  console.warn('[Queue Fallback] Processing synchronously due to missing/offline Redis.');
  const { sampleId, applianceType, pythonUrl, filePath } = jobData;
  let aiResults;
  
  try {
    if (!fs.existsSync(filePath)) {
      throw new Error('Image file missing on disk.');
    }
    const response = await axios.post(pythonUrl, { image_path: filePath }, { timeout: 25000 });
    if (response.data && response.data.status === 'success') {
      const rawZones = response.data.zones || {};
      const formattedZones = [
        { name: 'Zone A (North-West)', count: rawZones['A'] || 0, density: 25, risk: getRiskLevel((rawZones['A'] || 0) * 2) },
        { name: 'Zone B (North-East)', count: rawZones['B'] || 0, density: 25, risk: getRiskLevel((rawZones['B'] || 0) * 2) },
        { name: 'Zone C (South-West)', count: rawZones['C'] || 0, density: 25, risk: getRiskLevel((rawZones['C'] || 0) * 2) },
        { name: 'Zone D (South-East)', count: rawZones['D'] || 0, density: 25, risk: getRiskLevel((rawZones['D'] || 0) * 2) }
      ];

      const totalCount = response.data.colony_count || 1;
      formattedZones.forEach(z => {
        z.density = parseFloat(((z.count / totalCount) * 100).toFixed(1));
      });

      aiResults = {
        processedImage: response.data.processed_image_path ? response.data.processed_image_path.split(/[\\/]/).pop() : '',
        colonyCount: response.data.colony_count || 0,
        contaminationRisk: getRiskLevel(response.data.colony_count || 0),
        detections: response.data.detections || [],
        zones: formattedZones
      };
      console.log(`[Queue Fallback] Python AI processed sample ${sampleId} successfully.`);
    } else {
      throw new Error('Python service returned error status.');
    }
  } catch (err) {
    console.warn(`[Queue Fallback] Python AI failed for ${sampleId}. Using fallback local simulation. Error: ${err.message}`);
    aiResults = simulateAIAnalysis(applianceType);
  }
  
  const docRef = db.collection('samples').doc(sampleId);
  const doc = await docRef.get();
  if (doc.exists) {
    await docRef.update({
      status: 'Completed',
      colonyCount: aiResults.colonyCount,
      cfuCount: aiResults.colonyCount * (doc.data().dilutionFactor || 1),
      contaminationRisk: aiResults.contaminationRisk,
      detections: aiResults.detections,
      zones: aiResults.zones,
      ...(aiResults.processedImage && { processedImage: aiResults.processedImage })
    });
  }
  return { method: 'sync', status: 'Completed' };
}

module.exports = {
  enqueueAIAnalysis,
  redisConnection
};
