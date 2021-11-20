10     BORDER 0: PAPER 0: INK 7 : CLEAR 29999
20     SAVE "m:junk" CODE 1,1
30     ERASE "m:": REM clear drive M: without asking (Y/N)
40     LET s=16: LET st2=32: LET st35=56: LET s2=8: LET s4=4: LET s8=2: LET s34=12
50     INK 4: BRIGHT 1: GO SUB 310
60     FOR i=1 TO 6
70     PRINT  AT i+1,10+i; INK i;"GAMES  GAMES"
80     NEXT i
90     LOAD "allguys" CODE 30000
100    RANDOMISE USR 30000: REM draw the three characters
110    POKE 23739,78: POKE 23740,10: COPY "disk?" TO "M:": POKE 23739,244: POKE 23740,9: REM  the pokes prevent anytext being  printed by the COPY command
120    LOAD "allpik" CODE 30000
130    SAVE "m:gftg.pic" CODE 30000,6912
140    SAVE "m:ms.pic" CODE 36912,6912
150    SAVE "m:nomad.pic" CODE 43824,6912
160    LET f=1: REM number of program to run
170    PRINT  AT 15,0; INK 2; FLASH (f=1);"Gift from"; AT 16,0;"the Gods": LET x=30: LET y=0: INK 6*(f=1): GO SUB 520
180    PRINT  AT 10,11; INK 3; FLASH (f=2);"Mailstrom": LET x=115: INK 6*(f=2): GO SUB 520
190    PRINT  AT 15,24; INK 4; FLASH (f=3);"Nomad": LET x=205: INK 6*(f=3): GO SUB 520
200    LET a$= INKEY$ : IF a$="" THEN  GO TO 200
210    IF a$="7" OR a$=CHR$ (9) THEN  IF f<3 THEN  LET f=f+1: GO TO 170
220    IF a$="6" OR a$=CHR$ (8) THEN  IF f>1 THEN  LET f=f-1: GO TO 170
230    IF a$>="1" AND a$ <= "3" THEN  LET f= CODE a$-48: GO TO 170
240    IF a$=" " OR a$="0" THEN  GO TO 260
250    GO TO 170
260    LOAD "m:disk2" CODE 30000
270    RANDOMISE USR 30000
280    INK 0: PAPER 0
290    POKE 30000,f
300    LOAD "m:disk1"
310    REM draw +3
320    LET x=0: LET y=130: LET s=18
330    PLOT x,y
340    DRAW 0,s2: DRAW s,0: DRAW 0,s: DRAW s2,0: DRAW 0,-s: DRAW s,0: DRAW 0,-s2: DRAW -s,0: DRAW 0,-s: DRAW -s2,0: DRAW 0,s: DRAW -s,0
350    PLOT x+st35,y+s34
360    LET p4= PI /4
370    DRAW -s2,0
380    DRAW st2,0,- PI 
390    DRAW -s4,-s2, - P4
400    DRAW s/4,-s/2, - P4
410    DRAW -st2,0,- PI 
420    DRAW s2,0
430    PLOT x+st35,y+s34
440    DRAW s,0,- PI 
450    DRAW -s8,-s4,- P4
460    DRAW -s4,0
470    DRAW 0,-s2
480    DRAW s4,0
490    DRAW s8,-s4,- P4
500    DRAW -s,0,- PI 
510    RETURN 
520    REM draw arrow
530    PLOT x,y
540    DRAW 0,s: DRAW -s2,0: DRAW s34,s34: DRAW s34,-s34: DRAW -s2,0: DRAW 0,-s: DRAW -s2,0
550    RETURN 
