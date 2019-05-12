MIDI=0
AUDIO=0

all: FORCE
	javac -cp libraries/coremidi4j-1.1.jar:. *.java

FORCE:

run: FORCE
	java -cp libraries/coremidi4j-1.1.jar:. Synth ${MIDI} ${AUDIO}
