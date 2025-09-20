<?php
header('Content-Type: application/json');

$file = '/var/www/html/tracker/locations.json';
$max_positions = 500; // Máximo de posiciones por dispositivo

$input = file_get_contents('php://input');
$data = json_decode($input, true);

if (!isset($data['device_id']) || !isset($data['lat']) || !isset($data['lon'])) {
    echo json_encode(["status" => "error", "message" => "Datos incompletos"]);
    exit;
}

$locations = [];
if (file_exists($file)) {
    $locations = json_decode(file_get_contents($file), true);
}

if (!isset($locations[$data['device_id']])) {
    $locations[$data['device_id']] = [];
}

// Nueva posición con datos extra
$locations[$data['device_id']][] = [
    "lat" => floatval($data['lat']),
    "lon" => floatval($data['lon']),
    "accuracy" => isset($data['accuracy']) ? floatval($data['accuracy']) : null,
    "speed" => isset($data['speed']) ? intval($data['speed']) : null,
    "steps" => isset($data['steps']) ? intval($data['steps']) : null,
    "timestamp" => date("Y-m-d H:i:s")
];

// Mantener solo las últimas N posiciones
if (count($locations[$data['device_id']]) > $max_positions) {
    $locations[$data['device_id']] = array_slice($locations[$data['device_id']], -$max_positions);
}

file_put_contents($file, json_encode($locations, JSON_PRETTY_PRINT));
echo json_encode(["status" => "ok"]);
