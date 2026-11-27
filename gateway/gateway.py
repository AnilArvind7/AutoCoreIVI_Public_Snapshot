from websockets.server import serve
import asyncio
import json
import logging
from datetime import datetime, timezone

from websockets.exceptions import ConnectionClosed

HOST = "0.0.0.0"
PORT = 8765

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

vehicle_state = {
    "type": "vehicle_state",
    "vehicleId": "AC-001",
    "speed": 0,
    "battery": 82,
    "ignitionOn": True,
    "headlightsOn": False,
    "headlightBeam": "LOW",
    "hvacOn": True,
    "temperature": 22,
    "gear": "P",
    "frontLeftDoorOpen": False,
    "frontRightDoorOpen": False,
    "rearLeftDoorOpen": False,
    "rearRightDoorOpen": False,
}

clients = set()


def validate_state(message):
    """Validate vehicle values before accepting an update."""
    ranges = {
        "speed": (0, 220),
        "battery": (0, 100),
        "temperature": (16, 30),
    }

    for key, (minimum, maximum) in ranges.items():
        value = message.get(key)

        # bool is a subclass of int in Python, so reject it explicitly.
        if isinstance(value, bool) or not isinstance(value, int):
            return False, f"{key} must be an integer"

        if not minimum <= value <= maximum:
            return False, f"{key} must be between {minimum} and {maximum}"


    for key in (
    "ignitionOn",
    "headlightsOn",
    "hvacOn",
    "frontLeftDoorOpen",
    "frontRightDoorOpen",
    "rearLeftDoorOpen",
    "rearRightDoorOpen",
    ):
        if not isinstance(message.get(key), bool):
            return False, f"{key} must be a boolean"

    if "gear" in message:
        if message["gear"] not in ("P", "R", "N", "D"):
            return False, "gear must be P, R, N or D"

    if "headlightBeam" in message:
        if message["headlightBeam"] not in ("LOW", "HIGH"):
            return False, "headlightBeam must be LOW or HIGH"
    return True, ""


async def broadcast_state():
    """Send the latest state to all connected clients."""
    if not clients:
        return

    payload = json.dumps(vehicle_state)

    results = await asyncio.gather(
        *(client.send(payload) for client in list(clients)),
        return_exceptions=True,
    )

    for client, result in zip(list(clients), results):
        if isinstance(result, Exception):
            logging.warning("Could not send update: %s", result)


async def handle_client(websocket):
    clients.add(websocket)
    address = websocket.remote_address

    logging.info("Client connected: %s", address)

    try:
        # Send current state immediately after connection.
        await websocket.send(json.dumps(vehicle_state))

        async for raw_message in websocket:
            try:
                message = json.loads(raw_message)
            except json.JSONDecodeError:
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": "Invalid JSON",
                }))
                continue

            if not isinstance(message, dict):
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": "Expected a JSON object",
                }))
                continue

            if message.get("type") != "vehicle_state":
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": "Unsupported message type",
                }))
                continue

            valid, error = validate_state(message)

            if not valid:
                await websocket.send(json.dumps({
                    "type": "error",
                    "message": error,
                }))
                continue

            # Update only the approved fields.
            for key in (
                "speed",
                "battery",
                "ignitionOn",
                "headlightsOn",
                "hvacOn",
                "temperature",
                "frontLeftDoorOpen",
                "frontRightDoorOpen",
                "rearLeftDoorOpen",
                "rearRightDoorOpen"
            ):
                vehicle_state[key] = message[key]

            if "gear" in message:
                vehicle_state["gear"] = message["gear"]

            if "headlightBeam" in message:
                vehicle_state["headlightBeam"] = message["headlightBeam"]

            vehicle_state["updatedAt"] = (
                datetime.now(timezone.utc).isoformat()
            )

            logging.info(
               "Vehicle update: speed=%s battery=%s%% gear=%s headlight=%s beam=%s",
                vehicle_state["speed"],
                vehicle_state["battery"],
                vehicle_state["gear"],
                vehicle_state["headlightsOn"],
                vehicle_state["headlightBeam"],
            )
            logging.info(
                "Vehicle update: speed=%s battery=%s%% gear=%s "
                "headlights=%s beam=%s doors=[FL:%s FR:%s RL:%s RR:%s]",
                vehicle_state["speed"],
                vehicle_state["battery"],
                vehicle_state["gear"],
                vehicle_state["headlightsOn"],
                vehicle_state["headlightBeam"],
                vehicle_state["frontLeftDoorOpen"],
                vehicle_state["frontRightDoorOpen"],
                vehicle_state["rearLeftDoorOpen"],
                vehicle_state["rearRightDoorOpen"],
            )

            # All connected dashboards receive the latest state.
            await broadcast_state()

    except ConnectionClosed:
        logging.info("Client disconnected: %s", address)

    finally:
        clients.discard(websocket)


async def main():
    logging.info("AutoCore IVI Vehicle Gateway")
    logging.info("Listening on ws://%s:%s", HOST, PORT)

    async with serve(
        handle_client,
        HOST,
        PORT,
        max_size=16 * 1024,
        ping_interval=20,
        ping_timeout=20,
    ):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Gateway stopped")
