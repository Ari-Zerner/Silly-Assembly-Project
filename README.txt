Silly Assembly Project is a project I did for a programming class. It is an assembler, virtual machine, and IDE for a simple assembly language invented by the teacher of the class, Mr. Stulin. I have included several example programs in the assembly language. The language specification is as follows:

Directive	Parameters	Description
.start		label		The program should start execution at memory location label.
.end		<none>		Assembly ends.
.integer	#XXX		Place the integer XXX into memory at the specified location.
.allocate	#XXX		Allocate XXX memory locations.
.string		"text"		Allocate enough memory locations to hold the string one character per location.
				One extra location is also allocated. The first allocated location will contain the
				length of the string.

Code	Instruction	Parameters	Description
0	halt		<none>		Halt the program.
1	clrr		rX		Clear the contents of rX.
2	clrx		rX		Clear the memory location specified by rX.
3	clrm		label		Clear the memory location specified by label.
4	clrb		rX rY		Clear a block of memory. The starting location is specified by rX, the count by rY.
5	movir		#XXX rY		Move XXX to rY.
6	movrr		rX rY		Move the contents of rX to rY.
7	movrm		rX label	Move the contents of rX to the memory location specified by label.
8	movmr		label rX	Move the contents of the memory location specified by label to rX.
9	movxr		rX rY		Move the memory location specified by rX to rY.
10	movar		label rX	Move the address of label to rX.
11	movb		rX rY rZ	Move a block of memory. The source is specified by rX, the destination by rY, and the count by rZ.
12	addir		#XXX rY		Add XXX to rY.
13	addrr		rX rY		Add the contents of rX to rY.
14	addmr		label rX	Add the contents of the memory location specified by label to rX.
15	addxr		rX rY		Add the contents of the memory location specified by rY to rX.
16	subir		#XXX rY		Subtract XXX from rY.
17	subrr		rX rY		Subtract the contents of rX from rY.
18	submr		label rX	Subtract the contents of the memory location specified by label from rX.
19	subxr		rX rY		Subtract the contents of the memory location specified by rY from rX.
20	mulir		#XXX rY		Multiply rY by XXX.
21	mulrr		rX rY		Multiply rY by the contents of rX.
22	mulmr		label rX	Multiply rX by the contents of the memory location specified by label.
23	mulxr		rX rY		Multiply rX by the contents of the memory location specified by rY.
24	divir		#XXX rY		Divide rY by XXX.
25	divrr		rX rY		Divide rY by the contents of rX.
26	divmr		label rX	Divide rX by the contents of the memory location specified by label.
27	divxr		rX rY		Divide rX by the contents of the memory location specified by rY.
28	jmp		label		Jump to memory location label.
29	sojz		rX label	Subtract one from rX. Jump to label if result is zero.
30	sojnz		rX label	Subtract one from rX. Jump to label if result is not zero.
31	aojz		rX label	Add one to rX. Jump to label if result is zero.
32	aojnz		rX label	Add one to rX. Jump to label if result is not zero.
33	cmpir		#XXX rY		Move the contents of rY minus XXX to the compare register.
34	cmprr		#rX rY		Move the contents of rY minus the contents of rX to the compare register.
35	cmpmr		label rY	Move the contents of rY minus the contents of the memory location specified by label to the compare register.
36	jmpn		label		Jump to memory location label if the contents of the compare register are negative.
37	jmpz		label		Jump to memory location label if the contents of the compare register are zero.
38	jmpp		label		Jump to memory location label if the contents of the compare register are positive.
39	jsr		label		Jump to subroutine label. r5-r9 will be saved on the stack.
40	ret		<none>		Return from subroutine.
41	push		rX		Push the contents of rX onto the stack.
42	pop		rX		Pop the top of the stack into rX.
43	stackc		rX		Puts the condition of the last push or pop in rX. 0 - ok, 1 - full, 2 - empty.
44	outci		#XXX		Output the ASCII value of XXX to the console.
45	outcr		rX		Output the ASCII value of the contents of rX to the console.
46	outcx		rX		Output the ASCII value of the contents of the memory location specified by rX the to the console.
47	outcb		rX rY		Output a block of characters. The starting location is specified by rX, the count by rY.
48	readi		rX rY		Read an integer from the console into rX. The condition is stored in rY. 0 - ok, 1 - error.
49	printi		rX		Print the integer in rX to the console.
50	readc		rX		Read a character from the console into rX.
51	readln		label rX	Read a line of text from the console starting at the memory location specified by label. Store the length of the line in rX.
52	brk		<none>		If debugging, break into debugger.
53	movrx		rX rY		Move the contents of rX into the memory location specified by rY.
54	movxx		rX rY		Move the contents of the memory location specified by rX into the memory location specified by rY.
55	outs		label		Output the string stored in label.
56	nop		<none>		No operation.
57	jmpne		label	Jump to 	memory location label if the contents of the compare register are not zero.