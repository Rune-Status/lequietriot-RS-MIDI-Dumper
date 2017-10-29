package application.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import application.util.Buffer;

public class MidiDecoder {

	private static final int EVENT_NOTE_ON = 0x0;
	private static final int EVENT_NOTE_OFF = 0x1;
	private static final int EVENT_CONTROL_CHANGE = 0x2;
	private static final int EVENT_PITCH_BEND = 0x3;
	private static final int EVENT_CHANNEL_PRESSURE = 0x4;
	private static final int EVENT_KEY_PRESSURE = 0x5;
	private static final int EVENT_PROGRAM_CHANGE = 0x6;
	private static final int EVENT_END_OF_TRACK = 0x07;
	private static final int EVENT_SET_TEMPO = 0x17;

	private static final int CONTROLLER_BANK_SELECT = 0;
	private static final int CONTROLLER_BANK_SELECT_LSB = 32;
	private static final int CONTROLLER_MODULATION_WHEEL = 1;
	private static final int CONTROLLER_MODULATION_WHEEL_LSB = 33;
	private static final int CONTROLLER_CHANNEL_VOLUME = 7;
	private static final int CONTROLLER_CHANNEL_VOLUME_LSB = 39;
	private static final int CONTROLLER_PAN = 10;
	private static final int CONTROLLER_PAN_LSB = 42;
	private static final int CONTROLLER_NRPN = 99;
	private static final int CONTROLLER_NRPN_LSB = 98;
	private static final int CONTROLLER_RPN = 101;
	private static final int CONTROLLER_RPN_LSB = 100;
	private static final int CONTROLLER_DAMPER_PEDAL = 64;
	private static final int CONTROLLER_PORTAMENDO = 65;
	private static final int CONTROLLER_ALL_SOUND_OFF = 120;
	private static final int CONTROLLER_RESET_ALL_CONTROLLERS = 121;
	private static final int CONTROLLER_ALL_NOTES_OFF = 123;

	private static final int TYPE_END_OF_TRACK = 0x2f;
	private static final int TYPE_SET_TEMPO = 0x51;
	
	public byte[] decoded;
	
public void decode(byte[] data, boolean strict) throws IOException {
	Buffer input = new Buffer(data);
    input.position(input.capacity() - 3);
    int tracks = input.getUInt8();
    int division = input.getUInt16();

    int length = 14 + tracks * 10;
    input.position(0);

    int tempoCount = 0;
    int controlChangeCount = 0;
    int noteOnCount = 0;
    int noteOffCount = 0;
    int pitchBendCount = 0;
    int channelPressureCount = 0;
    int keyPressureCount = 0;
    int programChangeCount = 0;

    int i;
    int j;
    int track = 0;
    while(track < tracks) {
        i = -1;
        while(true) {
            j = input.getUInt8();

            if(strict || i != j) {
                ++length;
            }

            i = j & 0xf;
            if(j == EVENT_END_OF_TRACK) {
                ++track;
                break;
            }

            if(j == EVENT_SET_TEMPO) {
                ++tempoCount;
            } else if(i == EVENT_NOTE_ON) {
                ++noteOnCount;
            } else if(i == EVENT_NOTE_OFF) {
                ++noteOffCount;
            } else if(i == EVENT_CONTROL_CHANGE) {
                ++controlChangeCount;
            } else if(i == EVENT_PITCH_BEND) {
                ++pitchBendCount;
            } else if(i == EVENT_CHANNEL_PRESSURE) {
                ++channelPressureCount;
            } else if(i == EVENT_KEY_PRESSURE) {
                ++keyPressureCount;
            } else {
                if(i != EVENT_PROGRAM_CHANGE) {
                    throw new RuntimeException();
                }
                ++programChangeCount;
            }
        }
    }

    length += 5 * tempoCount;
    length += 2 * (noteOnCount + noteOffCount + controlChangeCount + pitchBendCount + keyPressureCount);
    length += channelPressureCount + programChangeCount;

    int deltaTimeOffset = input.position();

    int deltaTimeStart = input.position();
    int eventCount = tracks + tempoCount + controlChangeCount + noteOnCount +
            noteOffCount + pitchBendCount + channelPressureCount +
            keyPressureCount + programChangeCount;
    for(int l = 0; l < eventCount; l++) {
        input.getVariableLength();
    }

    length += input.position() - deltaTimeStart;

    int controllerOffset = input.position();

    int modulationWheelCount = 0;
    int modulationWheelMsbCount = 0;
    int channelVolumeCount = 0;
    int channelVolumeLsbCount = 0;
    int panCount = 0;
    int panLsbCount = 0;
    int nrpnCount = 0;
    int nrpnLsbCount = 0;
    int rpnCount = 0;
    int rpnLsbCount = 0;
    int var26 = 0;
    int toggleCount = 0;
    int controller = 0;

    for(int k = 0; k < controlChangeCount; k++) {
        controller = controller + input.getUInt8() & 127;
        if(controller != CONTROLLER_BANK_SELECT && controller != CONTROLLER_BANK_SELECT_LSB) {
            if(controller == CONTROLLER_MODULATION_WHEEL) {
                ++modulationWheelCount;
            } else if(controller == CONTROLLER_MODULATION_WHEEL_LSB) {
                ++modulationWheelMsbCount;
            } else if(controller == CONTROLLER_CHANNEL_VOLUME) {
                ++channelVolumeCount;
            } else if(controller == CONTROLLER_CHANNEL_VOLUME_LSB) {
                ++channelVolumeLsbCount;
            } else if(controller == CONTROLLER_PAN) {
                ++panCount;
            } else if(controller == CONTROLLER_PAN_LSB) {
                ++panLsbCount;
            } else if(controller == CONTROLLER_NRPN) {
                ++nrpnCount;
            } else if(controller == CONTROLLER_NRPN_LSB) {
                ++nrpnLsbCount;
            } else if(controller == CONTROLLER_RPN) {
                ++rpnCount;
            } else if(controller == CONTROLLER_RPN_LSB) {
                ++rpnLsbCount;
            } else if(controller != CONTROLLER_DAMPER_PEDAL && controller != CONTROLLER_PORTAMENDO &&
                    controller != CONTROLLER_ALL_SOUND_OFF && controller != CONTROLLER_RESET_ALL_CONTROLLERS &&
                    controller != CONTROLLER_ALL_NOTES_OFF) {
                ++toggleCount;
            } else {
                ++var26;
            }
        } else {
            ++programChangeCount;
        }
    }

    int opcodeOffset = 0;

    int var30 = input.position();
    input.skip(var26);

    int keyPressureOffset = input.position();
    input.skip(keyPressureCount);

    int channelPressureOffset = input.position();
    input.skip(channelPressureCount);

    int var33 = input.position();
    input.skip(pitchBendCount);

    int modulationWheelOffset = input.position();
    input.skip(modulationWheelCount);

    int channelVolumeOffset = input.position();
    input.skip(channelVolumeCount);

    int panOffset = input.position();
    input.skip(panCount);

    int keyOffset = input.position();
    input.skip(noteOnCount + noteOffCount + keyPressureCount);

    int onVelocityOffset = input.position();
    input.skip(noteOnCount);

    int toggleOffset = input.position();
    input.skip(toggleCount);

    int offVelocityOffset = input.position();
    input.skip(noteOffCount);

    int modulationWheelLsbOffset = input.position();
    input.skip(modulationWheelMsbCount);

    int channelVolumeLsbOffset = input.position();
    input.skip(channelVolumeLsbCount);

    int panLsbOffset = input.position();
    input.skip(panLsbCount);

    int programChangeOffset = input.position();
    input.skip(programChangeCount);

    int var45 = input.position();
    input.skip(pitchBendCount);

    int nrpnOffset = input.position();
    input.skip(nrpnCount);

    int nrpnLsbOffset = input.position();
    input.skip(nrpnLsbCount);

    int rpnOffset = input.position();
    input.skip(rpnCount);

    int rpnLsbOffset = input.position();
    input.skip(rpnLsbCount);

    int tempoSetOffset = input.position();
    input.skip(tempoCount * 3);

    data = new byte[length];

    Buffer outputBuf = new Buffer(data);
    
    outputBuf.putInt32(0x4d546864);
    outputBuf.putInt32(6);
    outputBuf.putInt16(tracks > 1 ? 1 : 0);
    outputBuf.putInt16(tracks);
    outputBuf.putInt16(division);

    input.position(deltaTimeOffset);

    int[] controllers = new int[128];
    controller = 0;

    int channel = 0;
    int key = 0;
    int onVelocity = 0;
    int offVelocity = 0;
    int pitch = 0;
    int channelPressure = 0;
    int keyPressure = 0;

    track = 0;
    while(track < tracks) {
        outputBuf.putInt32(0x4d54726b);
        outputBuf.skip(4);

        int chunkStart = outputBuf.position();
        int type = -1;

        while(true) {
            int deltaTime = input.getVariableLength();
            outputBuf.putVariableLength(deltaTime);

            int opcode = input.getUInt8(opcodeOffset++);
            boolean channelUpdated = strict || type != opcode;
            type = opcode & 0xf;

            if(opcode == EVENT_END_OF_TRACK) {
                if(channelUpdated) {
                    outputBuf.putInt8(0xff);
                }

                outputBuf.putInt8(TYPE_END_OF_TRACK);                     
                outputBuf.putInt8(0);                                     
                outputBuf.putIntLength(outputBuf.position() - chunkStart);
                track++;
                break;
            }

            if(opcode == EVENT_SET_TEMPO) {
                if(channelUpdated) {
                    outputBuf.putInt8(0xff);
                }

                outputBuf.putInt8(TYPE_SET_TEMPO);                  
                outputBuf.putInt8(3);                                

                outputBuf.putInt8(input.getUInt8(tempoSetOffset++));
                outputBuf.putInt8(input.getUInt8(tempoSetOffset++));
                outputBuf.putInt8(input.getUInt8(tempoSetOffset++));
            } else {
                channel ^= opcode >> 4;
                if(type == EVENT_NOTE_ON) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b10010000 + channel);
                    }

                    key += input.getInt8(keyOffset++);
                    onVelocity += input.getInt8(onVelocityOffset++);
                    outputBuf.putInt8(key & 127);
                    outputBuf.putInt8(onVelocity & 127);
                } else if(type == EVENT_NOTE_OFF) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b10000000 + channel);
                    }

                    key += input.getInt8(keyOffset++);
                    offVelocity += input.getInt8(offVelocityOffset++);
                    outputBuf.putInt8(key & 127);
                    outputBuf.putInt8(offVelocity & 127);
                } else if(type == EVENT_CONTROL_CHANGE) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b10110000 + channel);
                    }

                    controller = controller + input.getInt8(controllerOffset++) & 127;
                    outputBuf.putInt8(controller);

                    byte delta;
                    if(controller != CONTROLLER_BANK_SELECT && controller != CONTROLLER_BANK_SELECT_LSB) {
                        if(controller == CONTROLLER_MODULATION_WHEEL) {
                            delta = input.getInt8(modulationWheelOffset++);
                        } else if(controller == CONTROLLER_MODULATION_WHEEL_LSB) {
                            delta = input.getInt8(modulationWheelLsbOffset++);
                        } else if(controller == CONTROLLER_CHANNEL_VOLUME) {
                            delta = input.getInt8(channelVolumeOffset++);
                        } else if(controller == CONTROLLER_CHANNEL_VOLUME_LSB) {
                            delta = input.getInt8(channelVolumeLsbOffset++);
                        } else if(controller == CONTROLLER_PAN) {
                            delta = input.getInt8(panOffset++);
                        } else if(controller == CONTROLLER_PAN_LSB) {
                            delta = input.getInt8(panLsbOffset++);
                        } else if(controller == CONTROLLER_NRPN) {
                            delta = input.getInt8(nrpnOffset++);
                        } else if(controller == CONTROLLER_NRPN_LSB) {
                            delta = input.getInt8(nrpnLsbOffset++);
                        } else if(controller == CONTROLLER_RPN) {
                            delta = input.getInt8(rpnOffset++);
                        } else if(controller == CONTROLLER_RPN_LSB) {
                            delta = input.getInt8(rpnLsbOffset++);
                        } else if(controller != CONTROLLER_DAMPER_PEDAL && controller != CONTROLLER_PORTAMENDO &&
                                controller != CONTROLLER_ALL_SOUND_OFF && controller != CONTROLLER_RESET_ALL_CONTROLLERS &&
                                controller != CONTROLLER_ALL_NOTES_OFF) {
                            delta = input.getInt8(toggleOffset++);
                        } else {
                            delta = input.getInt8(var30++);
                        }
                    } else {
                        delta = input.getInt8(programChangeOffset++);
                    }

                    int value = delta + controllers[controller];
                    controllers[controller] = value;
                    outputBuf.putInt8(value & 127);
                } else if(type == EVENT_PITCH_BEND) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b11100000 + channel);
                    }

                    pitch += input.getInt8(var45++);
                    pitch += input.getInt8(var33++) << 7;
                    outputBuf.putInt8(pitch & 127);
                    outputBuf.putInt8(pitch >> 7 & 127);
                } else if(type == EVENT_CHANNEL_PRESSURE) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b11010000 + channel);
                    }

                    channelPressure += input.getInt8(channelPressureOffset++);
                    outputBuf.putInt8(channelPressure & 127);
                } else if(type == EVENT_KEY_PRESSURE) {

                    if(channelUpdated) {
                        outputBuf.putInt8(0b10100000 + channel);
                    }

                    key += input.getInt8(keyOffset++);
                    keyPressure += input.getInt8(keyPressureOffset++);
                    outputBuf.putInt8(key & 127);
                    outputBuf.putInt8(keyPressure & 127);
                } else {
                    if(type != EVENT_PROGRAM_CHANGE) {
                        throw new RuntimeException("Unknown event type");
                    }

                    if(channelUpdated) {
                        outputBuf.putInt8(0b11000000 + channel);
                    }

                    outputBuf.putInt8(input.getInt8(programChangeOffset++));
	}
}
}
}decoded = outputBuf.array();
}
}