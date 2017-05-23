/* Klavye.sc - collectibg keyboard data and analysing it within the context of live coding performance

Examples of usage:
Klavye.initClass;
Klavye.start;// initialize data collection and analysis
Klavye.startTypingWindow; //initialize window
Klavye.stop;//stop data collection and analysis
Klavye.allowBroadcast = false;
Klavye.allowListener = false;
*/

Klavye {

	classvar <version;
	classvar <broadcast;
	classvar <wek;
	classvar <>allowTypingF;
	classvar <>allowDeletionF;
	classvar <>allowEvalF;
	classvar <>allowBroadcast;
	classvar <>allowListener;
	classvar <>wekinator; 
	

	*initClass {

		super.initClass;
			 version = "25 December 2016";
			 NetAddr.broadcastFlag = true;
			 broadcast = NetAddr.new("localhost",NetAddr.langPort);
 
                         wek = NetAddr.new("localhost", 6448);		
			 allowBroadcast = false;
			 allowListener = false;
			("TypingFeatures.sc: " ++ version).postln;

		StartUp.add {//////////////OSC listeners////////////////////
		
			OSCdef(\typingF,{
		    		|msg,time,addr,port|
    				if (allowListener, { 
				 msg})
				},"/coding/typing/typingF").permanent_(true);
				

			OSCdef(\deletionF,{
    				|msg,time,addr,port|    
				 if (allowListener, {				
				   msg}) 
   					},"/coding/typing/deletionF").permanent_(true);

			OSCdef(\evalF,{
   				 |msg,time,addr,port|
   				 if (allowListener, { 
				   msg})
				 },"/coding/typing/evaluationF").permanent_(true);


			OSCdef(\parametersF,{
   				 |msg,time,addr,port|
   				 if (allowListener, { 
				  msg})
				 },"/coding/typing/parametersF").permanent_(true);

			OSCdef(\changingF,{
   				 |msg,time,addr,port|
   				 if (allowListener, { 
				msg})
				 },"/coding/typing/changingF").permanent_(true);

			OSCdef(\newStatementF,{
   				 |msg,time,addr,port|
   				 if (allowListener, { 
				  msg})
				 },"/coding/typing/newStatementF").permanent_(true);


	
				///////broadcasting ~parameters, ~changing and ~newStatement////////

			OSCdef(\wekinator, {
 				   |msg, time, addr, port|
				   if (allowBroadcast, { 
 				   broadcast.sendMsg("/coding/typing/parametersF", msg[1].clip(0,1));//layer 1 publication/broadcast
				   broadcast.sendMsg("/coding/typing/changingF", msg[2].clip(0,1));//layer 1 publication/broadcast
   				   broadcast.sendMsg("/coding/typing/newStatementF", msg[3].clip(0,1));
			          
					})
				   }, "/wek/outputs").permanent_(true);		
			}
	
	
		}


	*start {
			    var training = Document.current;
				~keyData = List.new;
				~evalData = List.new;

			training.keyDownAction = { |doc char modifier ascii keycode|
			    var x,y,z,i,text;
			    var d = Dictionary.new;
			    text = doc.string;
			    d[\char] = char;
			    d[\time] = Main.elapsedTime;
			    d[\ascii] = ascii;
			    d[\mod] = modifier;
			    d[\keycode] = keycode;
			    d[\posBefore] = if(char.isPrint,doc.selectionStart-2,doc.selectionStart-1);
				d[\posBefore] = if(d[\posBefore]>=0,d[\posBefore],nil);
			    d[\posAfter] = doc.selectionStart;
			    d[\posAfter] = if(d[\posAfter]==text.size,nil,d[\posAfter]);
			    d[\currentLine]= if ((char.isPrint) || (char.isPunct), doc.currentLine, nil);
			    d[\posInDoc] =  if (doc.selectionStart < text.size && not(doc.currentLine.isEmpty), d[\currentLine], nil);
			    d[\posInEmptyLine] = if ((doc.selectionStart < text.size) && (doc.currentLine.isEmpty), d[\currentLine], {nil});
			    d[\endOfDocPos] = if (doc.selectionStart == text.size, d[\currentLine], nil);
			    d[\charBefore] = if(d[\posBefore].notNil,{text[d[\posBefore]]},nil);
			    d[\charAfter] = if(d[\posAfter].notNil,{text[d[\posAfter]]},nil);

				// find continuous block of alpha characters before current position
			    i = doc.selectionStart-1; //select only letters not numbers
			    while( {
				if(i>=0,
						{(text[i].isAlpha || text[i].isSpace) && not (text[i].isUpper) },
				    false
				);
			    }, {
				i = i-1;
			    });



			    d[\alphasBefore] = if( i == (doc.selectionStart-1),nil,text[(i+1)..(doc.selectionStart-1)]);
			    d[\alphasBeforePos] = if(d[\alphasBefore].notNil,i+1,nil);//returns 0?

				// find continuous block of alpha characters after current position
			    i = doc.selectionStart;
			    while( {
				if(i<text.size,
						{(text[i].isAlpha || text[i].isSpace) && not (text[i].isUpper)}, //returns true
				    false
				);
			    }, {
				i = i+1;
			    });

			    d[\alphasAfter] = if( i == doc.selectionStart,nil,text[doc.selectionStart..(i-1)]);
			    d[\alphasAfterPos] = if(d[\alphasAfter].notNil,i-1,nil);//returns 9?

				d[\alphaToken] = if(d[\alphasBefore].notNil,{
				d[\alphasBefore]++if(d[\alphasAfter].notNil,d[\alphasAfter],"");
			    },{
				if(d[\alphasAfter].notNil,d[\alphasAfter],nil);
			    });

				//////////////////////////char before alpha token
			    d[\charBeforeAlphaTokenPos] = if(d[\alphaToken].notNil,{
				if(d[\alphasBeforePos].notNil,
				    {if(d[\alphasBeforePos]>0,d[\alphasBeforePos]-1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart-1,nil)
				);
			    },{
				nil;
			    });

			    d[\charBeforeAlphaToken] = if(d[\charBeforeAlphaTokenPos].notNil,
					{text[d[\charBeforeAlphaTokenPos]]},
					nil
			    );

			//////////////////////////char after alpha token

				d[\charAfterAlphaTokenPos] = if(d[\alphaToken].notNil,{
				if(d[\alphasAfterPos].notNil,
				    {if(d[\alphasAfterPos]>0,d[\alphasAfterPos]+1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart+1,nil)
				);
			    },{
				nil;
			    });

			    d[\charAfterAlphaToken] = if(d[\charAfterAlphaTokenPos].notNil,
				{ text[d[\charAfterAlphaTokenPos]]},
				nil
			    );

				//d[\typingAfterLastChar] = if (d[\charAfterAlphaToken].notNil,
					//{text[d[\currentLine]]}, nil);

			//////////////////////////////////////Numbers token//////////////////////////
					// find continuous block of numbers before current position

				 i = doc.selectionStart-1; //select only numbers Not letters
			    while( {
				if(i>=0,
						{not (text[i].isAlpha || text[i].isSpace || text[i].isPunct)},
				    false
				);
			    }, {
				i = i-1;
			    });

				d[\numbersBefore] = if( i == (doc.selectionStart-1),nil,text[(i+1)..(doc.selectionStart-1)]);
				d[\numbersBeforePos] = if(d[\numbersBefore].notNil,i+1,nil);//returns 0?
				//[d[\numbersBefore], d[\numbersBeforePos]].postln;


				// find continuous block of numbers after current position
			    i = doc.selectionStart;
			    while( {
				if(i<text.size,
						{not (text[i].isAlpha || text[i].isSpace || text[i].isPunct)}, //returns true
				    false
				);
			    }, {
				i = i+1;
			    });

			    d[\numbersAfter] = if( i == doc.selectionStart,nil,text[doc.selectionStart..(i-1)]);
			    d[\numbersAfterPos] = if(d[\numbersAfter].notNil,i-1,nil);

				d[\numberToken] = if(d[\numbersBefore].notNil,{
				d[\numbersBefore]++if(d[\numbersAfter].notNil,d[\numbersAfter],"");
			    },{
				if(d[\numbersAfter].notNil,d[\numbersAfter],nil);
			    });


				//////////////////////////char before number token
			    d[\charBeforeNumberTokenPos] = if(d[\numberToken].notNil,{
				if(d[\numbersBeforePos].notNil,
				    {if(d[\numbersBeforePos]>0,d[\numbersBeforePos]-1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart-1,nil)
				);
			    },{
				nil;
			    });

			    d[\charBeforeNumberToken] = if(d[\charBeforeNumberTokenPos].notNil,
					{text[d[\charBeforeNumberTokenPos]]},
					nil
			    );

			//////////////////////////char after number token

				d[\charAfterNumberTokenPos] = if(d[\numberToken].notNil,{
				if(d[\numbersAfterPos].notNil,
				    {if(d[\numbersAfterPos]>0,d[\numbersAfterPos]+1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart+1,nil)
				);
			    },{
				nil;
			    });

			    d[\charAfterNumberToken] = if(d[\charAfterNumberTokenPos].notNil,
				{ text[d[\charAfterNumberTokenPos]]},
				nil
			    );

				////////////////////////////////////////////////////////////////////////
				d[\equalNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $\=, {d[\numberToken]},{nil});
			    },{
				nil;
			    });

				d[\equalAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\=, {d[\alphaToken]},{nil});
			    },{
				nil;
			    });

				d[\upper] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken].isUpper,d[\charBeforeAlphaToken]++d[\alphaToken],nil);
			    },{
				nil;
			    });

			    d[\symbol] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $\\,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a backslash at the beginning of the parameter


				d[\parenthesisNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $(, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we have a parenthesis at the beginning of the phrase

				d[\parenthesisAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $(,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a parenthesis at the beginning of the phrase

				d[\sqBracketNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $[, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we have a brackets at the beginning of the phrase

				d[\sqBracketAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $[,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a brackets at the beginning of the phrase

				d[\function] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == ${,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a brackets at the beginning of the phrase

				d[\method] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\.,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a point at the beginning of the phrase


				d[\string] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $",d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a string at the beginning of the phrase

				d[\commaNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $,, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we are in a line with commas (parameter line)

				d[\commaAlpha] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\,,d[\alphaToken],nil);
			    },{
				nil;
				});//recognize when we are in a line with commas (parameter line)

				d[\tilde] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $~,d[\alphaToken],nil);
			    },{
				nil;
				});//recognize when positioned in a global parameter (parameter line)


				d[\semicolon] = if(d[\alphaToken].notNil &&  d[\charAfterAlphaToken].notNil,{
					if(d[\charAfterAlphaToken] == $;,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when I return to a previous idea

				//d[\charBeforeAlphaToken].postln;//
				//[d[\alphaToken],d[\alphasBeforePos],d[\charBeforeAlphaToken],  d[\symbol], d[\parenthesis], d[\sqBracket], d[\class], d[\tilde]].postln;
				//[d[\upper], d[\charBeforeAlphaToken]].postln;

				//[d[\alphaToken],d[\alphasAfterPos],d[\charAfterAlphaToken],   d[\semicolon]].postln;
				~samples1 = 10;
				~keyData = ~keyData.add(d).keep(-80);

			    // ~keyData = ~keyData.add([char, Main.elapsedTime, ascii, modifier, keycode, doc.currentLine, /*posAffected*/]);
			};

			thisProcess.interpreter.codeDump = { |code|
			    // runs everytime code is evaluated
			    var d = Dictionary.new;
			    d[\code] = code;
			    d[\time] = Main.elapsedTime;
			    ~evalData = ~evalData.add(d).keep(-80);
			};
			

			/////////////////low level analysis//////////////////////////////////////

			~shortTypingFfilter = Array.new;
			~typingFfilter = Array.new;
			~enterKeysFfilter = Array.new;
			~arrowKeysFfilter = Array.new;
			~deletionFfilter = Array.new;
			~equalSignsFfilter= Array.new;
			~evaluationFfilter = Array.new;
			~parenthesisFfilter = Array.new;
			~sqBracketsFfilter= Array.new;
			~curlyBracketsFfilter= Array.new;
			~qMarksFfilter = Array.new;
			~smallLettersFfilter = Array.new;
			~capitalLettersFfilter = Array.new;
			~numbersFfilter = Array.new;
			~periodFfilter= Array.new;
			~commasFfilter =Array.new;
			~semicolonsFfilter= Array.new;
			~operatorsFfilter= Array.new;
			~inSymbolTypingFfilter  = Array.new;
			~inParenthesisTypingFfilter = Array.new;
			~inArrayFfilter= Array.new;
			~inFunctionFfilter= Array.new;
			~inMethodFfilter= Array.new;
			~inStringFfilter= Array.new;
			~inSemicolonFfilter= Array.new;
			~inGlobalVarFfilter= Array.new;
			~inStoringVarFfilter = Array.new;
			~typinginEndofDocFfilter= Array.new;
			~inNumbersFfilter= Array.new;
			~inClassFfilter= Array.new;
			~modifyingPrevStatementFfilter = Array.new;
			~inParametersTypingFfilter = Array.new;
			~typinginEmptyLineFfilter = Array.new;

			~samples = 2;
			~timeWindowSize = 5; //number of seconds back in time to look at typing data, to use with no normalized data
			~timeWindowSizeShort = 0.5; //to use in normalized typeFrequency
			~analysisPeriod = 0.05;
		Tdef (\lowlevelAnalysis, {inf.do
		{
			~now = Main.elapsedTime; // to make
		
			// another low level feature extractors...
			//typingFrequency:
		
			// frequency of keypresses in shorter moving window of last ~typingWindowSize seconds -> ~typingF
			~lastNSecondsShortTypingF = ~keyData.collect({|x|x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSizeShort)}) - ~now;
			~howManyShortTypingF = ~lastNSecondsShortTypingF.size;
			~shortTypingFraw = ~howManyShortTypingF/~timeWindowSizeShort;
			~shortTypingFfilter = ~shortTypingFfilter.insert(0,~shortTypingFraw); //inserting frequencies
			~shortTypingFfilter = ~shortTypingFfilter.keep(~samples);
			~shortTypingFsmooth = ~shortTypingFfilter.mean; //freq average
			~shortTypingF = (~shortTypingFsmooth/24).clip(0,1);
			//{~shortTypingFInfo.value= ~shortTypingF}.defer;
		
			// frequency of keypresses in longer moving window of last ~typingWindowSize seconds -> ~typingF
		
			~lastNSecondsTypingF = ~keyData.collect({|x|x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howMany = ~lastNSecondsTypingF .size;
			~typingFraw = ~howMany/~timeWindowSize;
			~typingFfilter = ~typingFfilter.insert(0,~typingFraw);
			~typingFfilter = ~typingFfilter.keep(~samples);
			~typingFsmooth = ~typingFfilter.mean;
			~typingF = (~typingFsmooth/24).clip(0,1);
			//{~typingFInfo.value= ~typingF}.defer;
			//~typingF.postln;
		
			//"enterKey" sign frecuency by extracting ascii 13
		
			~lastNSecondsenterKeysF = ~keyData.collect({|x| if (x[\ascii] == 13, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyenterKeys = ~lastNSecondsenterKeysF.size;
			if (~typingF == 0, {~enterKeysFraw = 0}, {~enterKeysFraw = ~howManyenterKeys/~typingFsmooth});
			~enterKeysFfilter = ~enterKeysFfilter.insert(0,~enterKeysFraw);
			~enterKeysFfilter = ~enterKeysFfilter.keep(~samples);
			~enterKeysFsmooth = ~enterKeysFfilter.mean;
			~enterKeysF = ~enterKeysFsmooth;
			//{~enterKeysFInfo.value = ~enterKeysF}.defer;
		
		
			//"arrowKeys" sign frecuency by extracting keycode 65361, 65362, 65363, 65364
		
			~lastNSecondsarrowKeysF = ~keyData.collect({|x| if ((x[\keycode] == 65361) || (x[\keycode] == 65362) || (x[\keycode] == 65363) || (x[\keycode] == 65363), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyarrowKeys = ~lastNSecondsarrowKeysF.size;
			if (~typingF == 0, {~arrowKeysFraw = 0}, {~arrowKeysFraw = ~howManyarrowKeys/~typingFsmooth});
			~arrowKeysFfilter = ~arrowKeysFfilter.insert(0,~arrowKeysFraw);
			~arrowKeysFfilter = ~arrowKeysFfilter.keep(~samples);
			~arrowKeysFsmooth = ~arrowKeysFfilter.mean;
			~arrowKeysF = ~arrowKeysFsmooth;
			//{~arrowKeysFInfo.value = ~arrowKeysF}.defer;
			//equal sign frecuency by extracting ascii 61 "="
		
			~lastNSecondsequalSignsF = ~keyData.collect({|x| if (x[\ascii] == 61, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyequalSigns = ~lastNSecondsequalSignsF.size;
			if (~typingF == 0, {~equalSignsFraw = 0}, {~equalSignsFraw = ~howManyequalSigns/~typingFsmooth});
			~equalSignsFfilter = ~equalSignsFfilter.insert(0,~equalSignsFraw);
			~equalSignsFfilter = ~equalSignsFfilter.keep(~samples);
			~equalSignsFsmooth = ~equalSignsFfilter.mean;
			~equalSignsF = ~equalSignsFsmooth;
			//{~equalSignsFInfo.value = ~equalSignsF}.defer;
		
		
			//deletion frecuency by extracting ascii 8 & 127 (backspace and supr)
		
			~lastNSecondsDelF = ~keyData.collect({|x| if ((x[\ascii] == 8) || (x[\ascii] == 127), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyDeletes = ~lastNSecondsDelF.size;
			if (~typingF == 0, {~deletionFraw = 0}, {~deletionFraw = ~howManyDeletes/~typingFsmooth});
			~deletionFfilter = ~deletionFfilter.insert(0,~deletionFraw);
			~deletionFfilter = ~deletionFfilter.keep(~samples);
			~deletionFsmooth = ~deletionFfilter.mean;
			~deletionF = (~deletionFsmooth/24).clip(0,1);
			//{~deletionFInfo.value = ~deletionF}.defer;
		
			//evaluation Frequency
		
			~lastNSecondsevaluationF = ~evalData.collect({|x| x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyevaluations = ~lastNSecondsevaluationF.size;
			if (~typingF == 0, {~evaluationFraw = 0}, {~evaluationFraw = ~howManyevaluations/~typingFsmooth});
			~evaluationFfilter = ~evaluationFfilter.insert(0,~evaluationFraw);
			~evaluationFfilter= ~evaluationFfilter.keep(~samples);
			~evaluationFsmooth= ~evaluationFfilter.mean;
			~evaluationF = (~evaluationFsmooth/24).clip(0,1);
			//{~evaluationFInfo.value =  ~evaluationF}.defer;
		
		
			//number of parenthesis in last n seconds in Hz by extracting ascii code 40 & 41
		
			~lastNSecondsParenthesisF = ~keyData.collect({|x| if ((x[\ascii] == 40) || (x[\ascii] == 41), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
		
			~howManyParenthesis = ~lastNSecondsParenthesisF.size;
			if (~typingF == 0, {~parenthesisFraw = 0}, {~parenthesisFraw = ~howManyParenthesis/~typingFsmooth});
			~parenthesisFfilter = ~parenthesisFfilter.insert(0,~parenthesisFraw);
			~parenthesisFfilter= ~parenthesisFfilter.keep(~samples);
			~parenthesisFsmooth= ~parenthesisFfilter.mean;
			~parenthesisF= ~parenthesisFsmooth;
			//{~parenthesisFInfo.value =  ~parenthesisF}.defer;
		
			//number of square brackets in last n seconds in Hz by extracting ascii code 91 & 93
		
			~lastNSecondsSqBracketsF= ~keyData.collect({|x| if ((x[\ascii] == 91) || (x[\ascii] == 93), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManySqBrackets= ~lastNSecondsSqBracketsF.size;
			if (~typingF == 0, {~sqBracketsFraw = 0}, {~sqBracketsFraw = ~howManySqBrackets/~typingFsmooth});
			~sqBracketsFfilter = ~sqBracketsFfilter.insert(0,~sqBracketsFraw);
			~sqBracketsFfilter= ~sqBracketsFfilter.keep(~samples);
			~sqBracketsFsmooth= ~sqBracketsFfilter.mean;
			~sqBracketsF= ~sqBracketsFsmooth;
			//{~sqBracketsFInfo.value =  ~sqBracketsF}.defer;
		
		
			//number of curly brackets in last n seconds in Hz by extracting ascii code 123 & 125
		
			~lastNSecondsCurlyBracketsF= ~keyData.collect({|x| if ((x[\ascii] == 123) || (x[\ascii] == 125), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyCurlyBrackets= ~lastNSecondsCurlyBracketsF.size;
			if (~typingF == 0, {~curlyBracketsFraw = 0}, {~curlyBracketsFraw = ~howManyCurlyBrackets/~typingFsmooth});//limiting to zero
			~curlyBracketsFfilter = ~curlyBracketsFfilter.insert(0,~curlyBracketsFraw); //inserting frequencies
			~curlyBracketsFfilter= ~curlyBracketsFfilter.keep(~samples);
			~curlyBracketsFsmooth= ~curlyBracketsFfilter.mean;
			~curlyBracketsF= ~curlyBracketsFsmooth;
			//{~curlyBracketsFInfo.value =  ~curlyBracketsF}.defer;//posting data in window
		
			//number of quotation mark in last n seconds (Hz) by extracting ascii code 34
		
			~lastNSecondsQMarksF= ~keyData.collect({|x| if (x[\ascii] == 34, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyqMarks= ~lastNSecondsQMarksF.size;
			if (~typingF == 0, {~qMarksFraw = 0}, {~qMarksFraw = ~howManyqMarks/~typingFsmooth});//limiting to zero
			~qMarksFfilter = ~qMarksFfilter.insert(0,~qMarksFraw); //inserting deletion frequencies
			~qMarksFfilter= ~qMarksFfilter.keep(~samples);
			~qMarksFsmooth= ~qMarksFfilter.mean;
			~qMarksF= ~qMarksFsmooth/24;
			//{~qMarksFInfo.value =  ~qMarksF}.defer;//posting data in window
		
			//number of lower case letters in last n seconds (Hz) by extracting ascii code 97-122
		
			~lastNSecondssmallLettersF= ~keyData.collect({|x| if ((x[\ascii] >= 97) && (x[\ascii] <= 122), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManysmallLetters= ~lastNSecondssmallLettersF.size;
			if (~typingF == 0, {~smallLettersFraw = 0}, {~smallLettersFraw = ~howManysmallLetters/~typingFsmooth});//limiting to zero
			~smallLettersFfilter = ~smallLettersFfilter.insert(0,~smallLettersFraw); //inserting frequencies
			~smallLettersFfilter= ~smallLettersFfilter.keep(~samples);
			~smallLettersFsmooth= ~smallLettersFfilter.mean;
			~smallLettersF= ~smallLettersFsmooth;
			//{~smallLettersFInfo.value =  ~smallLettersF}.defer;//posting data in window
		
		
			//number of capital letters in last n seconds (Hz) by extracting ascii code 65-90
		
			~lastNSecondscapitalLettersF= ~keyData.collect({|x| if ((x[\ascii] >= 65) && (x[\ascii] <= 90), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManycapitalLetters= ~lastNSecondscapitalLettersF.size;
			if (~typingF == 0, {~capitalLettersFraw = 0}, {~capitalLettersFraw = ~howManycapitalLetters/~typingFsmooth});//limiting to zero
			~capitalLettersFfilter = ~capitalLettersFfilter.insert(0,~capitalLettersFraw); //inserting frequencies
			~capitalLettersFfilter= ~capitalLettersFfilter.keep(~samples);
			~capitalLettersFsmooth= ~capitalLettersFfilter.mean;
			~capitalLettersF= ~capitalLettersFsmooth;
			//{~capitalLettersFInfo.value =  ~capitalLettersF}.defer;//posting data in window
		
		
			//number of numbers in last n seconds (Hz) by extracting ascii code 48-57
		
			~lastNSecondsnumbersF= ~keyData.collect({|x| if ((x[\ascii] >= 48) && (x[\ascii] <= 57), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManynumbers= ~lastNSecondsnumbersF.size;
			if (~typingF == 0, {~numbersFraw = 0}, {~numbersFraw = ~howManynumbers/~typingFsmooth});//limiting to zero
			~numbersFfilter = ~numbersFfilter.insert(0,~numbersFraw); //inserting frequencies
			~numbersFfilter= ~numbersFfilter.keep(~samples);
			~numbersFsmooth= ~numbersFfilter.mean;
			~numbersF= ~numbersFsmooth;
			//{~numbersFInfo.value =  ~numbersF}.defer;//posting data in window
		
			//number of period in last n seconds (Hz) by extracting ascii code 48-57
		
			~lastNSecondsperiodF= ~keyData.collect({|x| if (x[\ascii] == 46, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyperiod= ~lastNSecondsperiodF.size;
			if (~typingF == 0, {~periodFraw = 0}, {~periodFraw = ~howManyperiod/~typingFsmooth});//limiting to zero
			~periodFfilter = ~periodFfilter.insert(0,~periodFraw); //inserting frequencies
			~periodFfilter= ~periodFfilter.keep(~samples);
			~periodFsmooth= ~periodFfilter.mean;
			~periodF= ~periodFsmooth;
			//{~periodFInfo.value =  ~periodF}.defer;//posting data in window
		
			//number of commas in last n seconds (Hz) by extracting ascii code 44
		
			~lastNSecondscommasF= ~keyData.collect({|x| if (x[\ascii] == 44, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManycommas= ~lastNSecondscommasF.size;
			if (~typingF == 0, {~commasFraw = 0}, {~commasFraw = ~howManycommas/~typingFsmooth});//limiting to zero
			~commasFfilter = ~commasFfilter.insert(0,~commasFraw); //inserting frequencies
			~commasFfilter= ~commasFfilter.keep(~samples);
			~commasFsmooth= ~commasFfilter.mean;
			~commasF= ~commasFsmooth;
			//{~commasFInfo.value =  ~commasF}.defer;//posting data in window
		
			//number of semicolons in last n seconds (Hz) by extracting ascii code 59
		
			~lastNSecondssemicolonsF= ~keyData.collect({|x| if (x[\ascii] == 59, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManysemicolons= ~lastNSecondssemicolonsF.size;
			if (~typingF == 0, {~semicolonsFraw = 0}, {~semicolonsFraw = ~howManysemicolons/~typingFsmooth});//limiting to zero
			~semicolonsFfilter = ~semicolonsFfilter.insert(0,~semicolonsFraw); //inserting frequencies
			~semicolonsFfilter= ~semicolonsFfilter.keep(~samples);
			~semicolonsFsmooth= ~semicolonsFfilter.mean;
			~semicolonsF= ~semicolonsFsmooth;
			//{~semicolonsFInfo.value =  ~semicolonsF}.defer;//posting data in window
		
			//number of basic operators (*,+,-, /) in last n seconds (Hz) by extracting ascii code 42,43,45,47
		
			~lastNSecondsoperatorsF=  ~keyData.collect({|x| if ((x[\ascii] == 42) || (x[\ascii] == 43) || (x[\ascii] == 45) ||(x[\ascii] == 47), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyoperators= ~lastNSecondsoperatorsF.size;
			if (~typingF == 0, {~operatorsFraw = 0}, {~operatorsFraw = ~howManyoperators/~typingFsmooth});//limiting to zero
			~operatorsFfilter = ~operatorsFfilter.insert(0,~operatorsFraw); //inserting frequencies
			~operatorsFfilter= ~operatorsFfilter.keep(~samples);
			~operatorsFsmooth= ~operatorsFfilter.mean;
			~operatorsF= ~operatorsFsmooth;
			//{~operatorsFInfo.value =  ~operatorsF}.defer;//posting data in window
		
		
			///////////////////////////////lexical analysis///////////////////
			//in Parameters:
		
			// in "symbol" typing
			~lastNSecondsinSymbolTypingF = ~keyData.collect ({|d| if (d[\symbol].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinSymbolTypingF= ~lastNSecondsinSymbolTypingF.size;
			if (~typingF == 0, {~inSymbolTypingFraw = 0}, {~inSymbolTypingFraw = ~howManyinSymbolTypingF/~typingFsmooth});//limiting to zero
			~inSymbolTypingFfilter = ~inSymbolTypingFfilter.insert(0,~inSymbolTypingFraw);
			~inSymbolTypingFfilter= ~inSymbolTypingFfilter.keep(~samples);
			~inSymbolTypingFsmooth= ~inSymbolTypingFfilter.mean;
			~inSymbolTypingF= ~inSymbolTypingFsmooth;
			//{~inSymbolTypingFInfo.value =  ~inSymbolTypingF}.defer;//posting data in window
		
		
			// in "parameters" typing
			~lastNSecondsinParametersTypingF = ~keyData.collect ({|d| if ((d[\commaNumber].notNil) || (d[\commaAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinParametersTypingF= ~lastNSecondsinParametersTypingF.size;
			if (~typingF == 0, {~inParametersTypingFraw = 0}, {~inParametersTypingFraw = ~howManyinParametersTypingF/~typingFsmooth});//limiting to zero
			~inParametersTypingFfilter = ~inParametersTypingFfilter.insert(0,~inParametersTypingFraw);
			~inParametersTypingFfilter= ~inParametersTypingFfilter.keep(~samples);
			~inParametersTypingFsmooth= ~inParametersTypingFfilter.mean;
			~inParametersTypingF= ~inParametersTypingFsmooth;
			//{~inParametersTypingFInfo.value =  ~inParametersTypingF}.defer;//posting data in window
		
			// in "parenthesis" typing
			~lastNSecondsinParenthesisTypingF = ~keyData.collect ({|d| if ((d[\parenthesisNumber].notNil)|| (d[\parenthesisAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinParenthesisTypingF= ~lastNSecondsinParenthesisTypingF.size;
			if (~typingF == 0, {~inParenthesisTypingFraw = 0}, {~inParenthesisTypingFraw = ~howManyinParenthesisTypingF/~typingFsmooth});//limiting to zero
			~inParenthesisTypingFfilter = ~inParenthesisTypingFfilter.insert(0,~inParenthesisTypingFraw);
			~inParenthesisTypingFfilter= ~inParenthesisTypingFfilter.keep(~samples);
			~inParenthesisTypingFsmooth= ~inParenthesisTypingFfilter.mean;
			~inParenthesisTypingF= ~inParenthesisTypingFsmooth;
			//{~inParenthesisTypingFInfo.value =  ~inParenthesisTypingF}.defer;//posting data in window
		
			// in "Array/List" typing
			~lastNSecondsinArrayF = ~keyData.collect ({|d| if ((d[\sqBracketNumber].notNil) || (d[\sqBracketAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinArrayF= ~lastNSecondsinArrayF.size;
			if (~typingF == 0, {~inArrayFraw = 0}, {~inArrayFraw = ~howManyinArrayF/~typingFsmooth});//limiting to zero
			~inArrayFfilter = ~inArrayFfilter.insert(0,~inArrayFraw);
			~inArrayFfilter= ~inArrayFfilter.keep(~samples);
			~inArrayFsmooth= ~inArrayFfilter.mean;
			~inArrayF= ~inArrayFsmooth;
			//{~inArrayFInfo.value =  ~inArrayF}.defer;//posting data in window
		
		
			//in token of "numbers"
		
			~lastNSecondsinNumbersF = ~keyData.collect ({|d| if (d[\numberToken].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinNumbersF= ~lastNSecondsinNumbersF.size;
			if (~typingF == 0, {~inNumbersFraw = 0}, {~inNumbersFraw = ~howManyinNumbersF/~typingFsmooth});//limiting to zero
			~inNumbersFfilter = ~inNumbersFfilter.insert(0,~inNumbersFraw);
			~inNumbersFfilter= ~inNumbersFfilter.keep(~samples);
			~inNumbersFsmooth= ~inNumbersFfilter.mean;
			~inNumbersF= ~inNumbersFsmooth;
			//{~inNumbersFInfo.value =  ~inNumbersF}.defer;//posting data in window
		
			//in "function" typing
		
			~lastNSecondsinFunctionF = ~keyData.collect ({|d| if (d[\function].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinFunctionF= ~lastNSecondsinFunctionF.size;
			if (~typingF == 0, {~inFunctionFraw = 0}, {~inFunctionFraw = ~howManyinFunctionF/~typingFsmooth});//limiting to zero
			~inFunctionFfilter = ~inFunctionFfilter.insert(0,~inFunctionFraw);
			~inFunctionFfilter= ~inFunctionFfilter.keep(~samples);
			~inFunctionFsmooth= ~inFunctionFfilter.mean;
			~inFunctionF= ~inFunctionFsmooth;
			//{~inFunctionFInfo.value =  ~inFunctionF}.defer;
		
		
			//in "method" typing by mapping period
		
			~lastNSecondsinMethodF = ~keyData.collect ({|d| if (d[\method].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinMethodF= ~lastNSecondsinMethodF.size;
			if (~typingF == 0, {~inMethodFraw = 0}, {~inMethodFraw = ~howManyinMethodF/~typingFsmooth});//limiting to zero
			~inMethodFfilter = ~inMethodFfilter.insert(0,~inMethodFraw);
			~inMethodFfilter= ~inMethodFfilter.keep(~samples);
			~inMethodFsmooth= ~inMethodFfilter.mean;
			~inMethodF= ~inMethodFsmooth;
			//{~inMethodFInfo.value =  ~inMethodF}.defer;
		
			//in "string" typing
		
			~lastNSecondsinStringF = ~keyData.collect ({|d| if (d[\string].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinStringF= ~lastNSecondsinStringF.size;
			if (~typingF == 0, {~inStringFraw = 0}, {~inStringFraw = ~howManyinStringF/~typingFsmooth});//limiting to zero
			~inStringFfilter = ~inStringFfilter.insert(0,~inStringFraw);
			~inStringFfilter= ~inStringFfilter.keep(~samples);
			~inStringFsmooth= ~inStringFfilter.mean;
			~inStringF= ~inStringFsmooth;
			//{~inStringFInfo.value =  ~inStringF}.defer;
		
			// NewStatement: adding structural information
		
			//in "class" typing
		
			~lastNSecondsinClassF = ~keyData.collect ({|d| if (d[\upper].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinClassF= ~lastNSecondsinClassF.size;
			if (~typingF == 0, {~inClassFraw = 0}, {~inClassFraw = ~howManyinClassF/~typingFsmooth});//limiting to zero
			~inClassFfilter = ~inClassFfilter.insert(0,~inClassFraw);
			~inClassFfilter= ~inClassFfilter.keep(~samples);
			~inClassFsmooth= ~inClassFfilter.mean;
			~inClassF= ~inClassFsmooth;
			//{~inClassFInfo.value =  ~inClassF}.defer;

		
			//typing in "end of doc". Typing at the end of the document.
		
			~lastNSecondsTypinginEndofDocF = ~keyData.collect ({|d|if (d[\endOfDocPos].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyTypinginEndofDocF= ~lastNSecondsTypinginEndofDocF.size;
		
			if (~typingF == 0, {~typinginEndofDocFraw = 0}, {~typinginEndofDocFraw = ~howManyTypinginEndofDocF/~typingFsmooth});//limiting to zero
		
			~typinginEndofDocFfilter = ~typinginEndofDocFfilter.insert(0,~typinginEndofDocFraw);
			~typinginEndofDocFfilter= ~typinginEndofDocFfilter.keep(~samples);
			~typinginEndofDocFsmooth= ~typinginEndofDocFfilter.mean;
			~typinginEndofDocF= ~typinginEndofDocFsmooth;
			//{~typinginEndofDocFInfo.value =  ~typinginEndofDocF}.defer;
		
			//typing in "previous empty line", adding new info in past "empty lines"
		
			~lastNSecondstypinginEmptyLineF = ~keyData.collect ({|d|if (d[\posInEmptyLine].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManytypinginEmptyLineF= ~lastNSecondstypinginEmptyLineF.size;
		
			if (~typingF == 0, {~typinginEmptyLineFraw = 0}, {~typinginEmptyLineFraw = ~howManytypinginEmptyLineF/~typingFsmooth});//limiting to zero
		
			~typinginEmptyLineFfilter = ~typinginEmptyLineFfilter.insert(0,~typinginEmptyLineFraw);
			~typinginEmptyLineFfilter= ~typinginEmptyLineFfilter.keep(~samples);
			~typinginEmptyLineFsmooth= ~typinginEmptyLineFfilter.mean;
			~typinginEmptyLineF= ~typinginEmptyLineFsmooth;
			//{~typinginEmptyLineFInfo.value =  ~typinginEmptyLineF}.defer;
		
		
			//in "global var"
			~lastNSecondsinGlobalVarF = ~keyData.collect ({|d| if (d[\tilde].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinGlobalVarF= ~lastNSecondsinGlobalVarF.size;
			if (~typingF == 0, {~inGlobalVarFraw = 0}, {~inGlobalVarFraw = ~howManyinGlobalVarF/~typingFsmooth});//limiting to zero
			~inGlobalVarFfilter = ~inGlobalVarFfilter.insert(0,~inGlobalVarFraw);
			~inGlobalVarFfilter= ~inGlobalVarFfilter.keep(~samples);
			~inGlobalVarFsmooth= ~inGlobalVarFfilter.mean;
			~inGlobalVarF= ~inGlobalVarFsmooth;
			//{~inGlobalVarFInfo.value =  ~inGlobalVarF}.defer;
		
		
			//in "storing variable" by extracting alpha token after "="
			~lastNSecondsinStoringVarF = ~keyData.collect ({|d| if ((d[\equalNumber].notNil) || (d[\equalAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinStoringVarF= ~lastNSecondsinStoringVarF.size;
			if (~typingF == 0, {~inStoringVarFraw = 0}, {~inStoringVarFraw = ~howManyinStoringVarF/~typingFsmooth});//limiting to zero
			~inStoringVarFfilter = ~inStoringVarFfilter.insert(0,~inStoringVarFraw);
			~inStoringVarFfilter= ~inStoringVarFfilter.keep(~samples);
			~inStoringVarFsmooth= ~inStoringVarFfilter.mean;
			~inStoringVarF= ~inStoringVarFsmooth;
			//{~inStoringVarFInfo.value =  ~inStoringVarF}.defer;
		
		
			////////////changes///////
			//in line with a "semicolon"
		
			~lastNSecondsinSemicolonF = ~keyData.collect ({|d| if (d[\semicolon].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinSemicolonF= ~lastNSecondsinSemicolonF.size;
			if (~typingF == 0, {~inSemicolonFraw = 0}, {~inSemicolonFraw = ~howManyinSemicolonF/~typingFsmooth});//limiting to zero
			~inSemicolonFfilter = ~inSemicolonFfilter.insert(0,~inSemicolonFraw);
			~inSemicolonFfilter= ~inSemicolonFfilter.keep(~samples);
			~inSemicolonFsmooth= ~inSemicolonFfilter.mean;
			~inSemicolonF= ~inSemicolonFsmooth;
			//{~inSemicolonFInfo.value =  ~inSemicolonF}.defer;
		
		
			//modifying "previous (written) statement"
		
			~lastNSecondsmodifyingPrevStatementF = ~keyData.collect ({|d|if (d[\posInDoc].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManymodifyingPrevStatementF= ~lastNSecondsmodifyingPrevStatementF.size;
			if (~typingF == 0, {~modifyingPrevStatementFraw = 0}, {~modifyingPrevStatementFraw = ~howManymodifyingPrevStatementF/~typingFsmooth});//limiting to zero
		
			~modifyingPrevStatementFfilter = ~modifyingPrevStatementFfilter.insert(0,~modifyingPrevStatementFraw);
			~modifyingPrevStatementFfilter= ~modifyingPrevStatementFfilter.keep(~samples);
			~modifyingPrevStatementFsmooth= ~modifyingPrevStatementFfilter.mean;
			~modifyingPrevStatementF= ~modifyingPrevStatementFsmooth;
			//{~modifyingPrevStatementInfo.value =  ~modifyingPrevStatementF}.defer;
		



			///////////////// broadcasting to network: ~typingF, ~deletionF, ~evalF///////////////
 			
			if (allowBroadcast, {
			broadcast.sendMsg("/coding/typing/typingF",~shortTypingF);
			broadcast.sendMsg("/coding/typing/deletionF",~deletionF);
			broadcast.sendMsg("/coding/typing/evaluationF",~evaluationF);
			/////////////////sending data to wekinator//////
			wek.sendMsg("/wek/inputs", ~enterKeysF, ~arrowKeysF, ~equalSignsF,  ~parenthesisF, ~sqBracketsF, ~curlyBracketsF, ~qMarksF, ~smallLettersF, ~capitalLettersF,  ~numbersF, ~periodF, ~commasF, ~semicolonsF, ~operatorsF, ~inSymbolTypingF, ~inParametersTypingF, ~inParenthesisTypingF, ~inArrayF, ~inNumbersF, ~inFunctionF, ~inMethodF, ~inStringF, ~inClassF, ~inGlobalVarF, ~inStoringVarF, ~inSemicolonF, ~typinginEndofDocF, ~typinginEmptyLineF, ~modifyingPrevStatementF );
		
});

		// wait to do it again...
			~analysisPeriod.wait;
		    }
		}).play;
	}


	*stop {
			Window.closeAll;
			Tdef (\lowlevelAnalysis).stop; 
	 	}


	*startTypingWindow  {

				/////////////////////////////////////////Window///////////////////////////////////////////
				 
				var  w=Window("window", Rect(100,100,930,500)).front, training;
				~lowLevelAnalysis=StaticText(w,Rect(60,10,200,50));
				~lowLevelAnalysis.string="-----low level analysis-------";

				//first block
				~typingFInfo = NumberBox(w, Rect(10,50,90,50));
				~typingFInfoString=StaticText(w,Rect(10,80,100,50));
				~typingFInfoString.string="TypingF";

				~enterKeysFInfo= NumberBox(w, Rect (110,50,90,50));
				~enterKeysFInfoString=StaticText(w,Rect(110,80,100,50));
				~enterKeysFInfoString.string="enterKeysF";

				~arrowKeysFInfo = NumberBox(w, Rect(210,50,90,50));
				~arrowKeysFInfoString=StaticText(w,Rect(210,80,100,50));
				~arrowKeysFInfoString.string="arrowKeysF";

				//second block
				~equalSignsFInfo = NumberBox(w, Rect(10,120,90,50));
				~equalSignsFInfoString=StaticText(w,Rect(10,150,90,50));
				~equalSignsFInfoString.string="equalSignsF";

				~sqBracketsFInfo = NumberBox(w, Rect(110,120,90,50));
				~sqBracketsFInfoString=StaticText(w,Rect(110,150,100,50));
				~sqBracketsFInfoString.string="sqBracketsF";

				~parenthesisFInfo= NumberBox(w, Rect(210,120,90,50));
				~parenthesisFInfoString=StaticText(w,Rect(210,150,90,50));
				~parenthesisFInfoString.string="parenthesisF";

				//third block
				~curlyBracketsFInfo = NumberBox(w, Rect(10,200,90,50));
				~curlyBracketsFInfoString=StaticText(w,Rect(10,230,90,50));
				~curlyBracketsFInfoString.string="curlyBracketsF";

				~qMarksFInfo = NumberBox(w, Rect(110,200,90,50));
				~qMarksFInfoString=StaticText(w,Rect(120,230,90,50));
				~qMarksFInfoString.string="qMarksF";

				~smallLettersFInfo = NumberBox(w, Rect(210,200,90,50));
				~smallLettersFInfoString=StaticText(w,Rect(210,230,110,50));
				~smallLettersFInfoString.string="smallLettersF";

				//fourth block
				~capitalLettersFInfo = NumberBox(w, Rect(10,290,90,50));
				~capitalLFInfoString=StaticText(w,Rect(10,320,90,50));
				~capitalLFInfoString.string="capitallettersF";

				~numbersFInfo = NumberBox(w, Rect(110,290,90,50));
				~numbersFInfoString=StaticText(w,Rect(110,320,90,50));
				~numbersFInfoString.string="numbersF";

				~periodFInfo= NumberBox(w, Rect(210,290,90,50));
				~periodFInfoString=StaticText(w,Rect(210,320,90,50));
				~periodFInfoString.string="periodF";

				//fifth block
				~commasFInfo = NumberBox(w, Rect(10,380,90,50));
				~numbersFInfoString=StaticText(w,Rect(10,410,90,50));
				~numbersFInfoString.string="commasF";

				~semicolonsFInfo = NumberBox(w, Rect(110,380,90,50));
				~semicolonsFInfoString=StaticText(w,Rect(110,410,90,50));
				~semicolonsFInfoString.string="semicolonsF";

				~operatorsFInfo= NumberBox(w, Rect(210,380,90,50));
				~operatorsFInfoString=StaticText(w,Rect(210,410,90,50));
				~operatorsFInfoString.string="operatorsF";


				//////////////////////lexical analysis////////////////

				~contextualAnalysis=StaticText(w,Rect(400,10,220,50));
				~contextualAnalysis.string="-----lexical analysis-------";


				//first block
				~inSymbolTypingFInfo= NumberBox(w, Rect(350,50,90,50));
				~inSymbolTypingFInfoString=StaticText(w,Rect(350,80,110,50));
				~inSymbolTypingFInfoString.string= "inSymbolF";

				~inParenthesisTypingFInfo= NumberBox(w, Rect(450,50,90,50));
				~inParenthesisTypingFInfoString=StaticText(w,Rect(440,80,120,50));
				~inParenthesisTypingFInfoString.string="inParenthesisF";


				~inArrayFInfo= NumberBox(w, Rect(550,50,90,50));
				~inArrayFInfoString=StaticText(w,Rect(560,80,120,50));
				~inArrayFInfoString.string="inArrayF";

				//second block
				~inFunctionFInfo= NumberBox(w, Rect(350,120,90,50));
				~inFunctionFInfoString=StaticText(w,Rect(355,150,120,50));
				~inFunctionFInfoString.string= "inFunctionF" ;

				~inMethodFInfo= NumberBox(w, Rect(450,120,90,50));
				~inMethodFInfoString=StaticText(w,Rect(460,150,120,50));
				~inMethodFInfoString.string="inMethodF";

				~inStringFInfo= NumberBox(w, Rect(550,120,90,50));
				~inStringFInfoString=StaticText(w,Rect(560,150,120,50));
				~inStringFInfoString.string="inStringF";


				//third block

				~inParametersTypingFInfo= NumberBox(w, Rect(350,200,90,50));
				~inParametersTypingFInfoString=StaticText(w,Rect(350,230,120,50));
				~inParametersTypingFInfoString.string= "inParameters";


				~inSemicolonFInfo= NumberBox(w, Rect(450,200,90,50));
				~inSemicolonFInfoString=StaticText(w,Rect(450,230,120,50));
				~inSemicolonFInfoString.string= "inSemicolonF" ;

				~inClassFInfo = NumberBox(w, Rect(550,200,90,50));
				~inClassFInfoString=StaticText(w,Rect(560,230,90,50));
				~inClassFInfoString.string="inClassF";

				//fourth block

				~inNumbersFInfo= NumberBox(w, Rect(350,290,90,50));
				~inNumbersFInfoString=StaticText(w,Rect(350,320,120,50));
				~inNumbersFInfoString.string= "inNumbersF";

				~inGlobalVarFInfo= NumberBox(w, Rect(450,290,90,50));
				~inGlobalVarFInfoString=StaticText(w,Rect(450,320,120,50));
				~inGlobalVarFInfoString.string= "inGlobalVarF";

				~inStoringVarFInfo= NumberBox(w, Rect(550,290,90,50));
				~inStoringVarFInfoString=StaticText(w,Rect(550,320,120,50));
				~inStoringVarFInfoString.string= "inStoringVarF";

				//fifth block

				~modifyingPrevStatementInfo= NumberBox(w, Rect(350,380,90,50));
				~modifyingPrevStatementInfoString=StaticText(w,Rect(320,410,220,50));
				~modifyingPrevStatementInfoString.string= "inPrevStatement";

				~typinginEmptyLineFInfo= NumberBox(w, Rect(450,380,90,50));
				~typinginEmptyLineFInfoString=StaticText(w,Rect(450,410,220,50));
				~typinginEmptyLineFInfoString.string= "inEmptyLine";

				~typinginEndofDocFInfo= NumberBox(w, Rect(550,380,90,50));
				~typinginEndofDocFInfoString=StaticText(w,Rect(550,410,220,50));
				~typinginEndofDocFInfoString.string= "inEndOfDocF";

				//////////////////////high level analysis////////////////

				~highLevelAnalysis=StaticText(w,Rect(700,10,200,50));
				~highLevelAnalysis.string="-----high level analysis-------";

				~shortTypingFInfo = NumberBox(w, Rect (700,50,90,50));
				~shortTypingFInfoString=StaticText(w,Rect(700,80,100,50));
				~shortTypingFInfoString.string="shortTypingF";

				~deletionFInfo = NumberBox(w, Rect(800, 50,90,50));
				~deletionFInfoString=StaticText(w,Rect(800,80,100,50));
				~deletionFInfoString.string="deletionF";

				~evaluationFInfo = NumberBox(w, Rect(700,150,90,50));
				~evaluationFInfoString=StaticText(w,Rect(700,180,90,50));
				~evaluationFInfoString.string="evaluationF";

				~inParameterFInfo = NumberBox(w, Rect(800,150,90,50));
				~inParameterFInfoString=StaticText(w,Rect(800,180,90,50));
				~inParameterFInfoString.string="inParameter";

				~changingFInfo = NumberBox(w, Rect(700,250,90,50));
				~changingFInfoString=StaticText(w,Rect(700,280,90,50));
				~changingFInfoString.string="changingF";

				~newStatementFInfo = NumberBox(w, Rect(800,250,90,50));
				~newStatementFInfoString=StaticText(w,Rect(800,280,120,50));
				~newStatementFInfoString.string="newStatementF";
				///////////////////broadcasting to wekinator///////////////

			OSCdef(\wekinator, {
 				   |msg, time, addr, port|
				   if (allowBroadcast, { 
 				   broadcast.sendMsg("/coding/typing/parametersF", msg[1].clip(0,1));//layer 1 publication/broadcast
				   broadcast.sendMsg("/coding/typing/changingF", msg[2].clip(0,1));//layer 1 publication/broadcast
   				   broadcast.sendMsg("/coding/typing/newStatementF", msg[3].clip(0,1));
			           {~inParameterFInfo.value =  msg[1].clip(0,1)}.defer;
                                   { ~changingFInfo.value =  msg[2].clip(0,1)}.defer;
                                   { ~newStatementFInfo.value =  msg[3].clip(0,1)}.defer;
					})
				   }, "/wek/outputs").permanent_(true);	
				///////////////////////////////////////////data collection/////////////////////
			training=  Document.current;
			~keyData = List.new;
			~evalData = List.new;
				

			training.keyDownAction = { |doc char modifier ascii keycode|
			    var x,y,z,i,text;
			    var d = Dictionary.new;
			    text = doc.string;
			    d[\char] = char;
			    d[\time] = Main.elapsedTime;
			    d[\ascii] = ascii;
			    d[\mod] = modifier;
			    d[\keycode] = keycode;
			    d[\posBefore] = if(char.isPrint,doc.selectionStart-2,doc.selectionStart-1);
				d[\posBefore] = if(d[\posBefore]>=0,d[\posBefore],nil);
			    d[\posAfter] = doc.selectionStart;
			    d[\posAfter] = if(d[\posAfter]==text.size,nil,d[\posAfter]);
			    d[\currentLine]= if ((char.isPrint) || (char.isPunct), doc.currentLine, nil);
			    d[\posInDoc] =  if (doc.selectionStart < text.size && not(doc.currentLine.isEmpty), d[\currentLine], nil);
			    d[\posInEmptyLine] = if ((doc.selectionStart < text.size) && (doc.currentLine.isEmpty), d[\currentLine], {nil});
			    d[\endOfDocPos] = if (doc.selectionStart == text.size, d[\currentLine], nil);
			    d[\charBefore] = if(d[\posBefore].notNil,{text[d[\posBefore]]},nil);
			    d[\charAfter] = if(d[\posAfter].notNil,{text[d[\posAfter]]},nil);

				// find continuous block of alpha characters before current position
			    i = doc.selectionStart-1; //select only letters not numbers
			    while( {
				if(i>=0,
						{(text[i].isAlpha || text[i].isSpace) && not (text[i].isUpper) },
				    false
				);
			    }, {
				i = i-1;
			    });



			    d[\alphasBefore] = if( i == (doc.selectionStart-1),nil,text[(i+1)..(doc.selectionStart-1)]);
			    d[\alphasBeforePos] = if(d[\alphasBefore].notNil,i+1,nil);//returns 0?

				// find continuous block of alpha characters after current position
			    i = doc.selectionStart;
			    while( {
				if(i<text.size,
						{(text[i].isAlpha || text[i].isSpace) && not (text[i].isUpper)}, //returns true
				    false
				);
			    }, {
				i = i+1;
			    });

			    d[\alphasAfter] = if( i == doc.selectionStart,nil,text[doc.selectionStart..(i-1)]);
			    d[\alphasAfterPos] = if(d[\alphasAfter].notNil,i-1,nil);//returns 9?

				d[\alphaToken] = if(d[\alphasBefore].notNil,{
				d[\alphasBefore]++if(d[\alphasAfter].notNil,d[\alphasAfter],"");
			    },{
				if(d[\alphasAfter].notNil,d[\alphasAfter],nil);
			    });

				//////////////////////////char before alpha token
			    d[\charBeforeAlphaTokenPos] = if(d[\alphaToken].notNil,{
				if(d[\alphasBeforePos].notNil,
				    {if(d[\alphasBeforePos]>0,d[\alphasBeforePos]-1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart-1,nil)
				);
			    },{
				nil;
			    });

			    d[\charBeforeAlphaToken] = if(d[\charBeforeAlphaTokenPos].notNil,
					{text[d[\charBeforeAlphaTokenPos]]},
					nil
			    );

			//////////////////////////char after alpha token

				d[\charAfterAlphaTokenPos] = if(d[\alphaToken].notNil,{
				if(d[\alphasAfterPos].notNil,
				    {if(d[\alphasAfterPos]>0,d[\alphasAfterPos]+1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart+1,nil)
				);
			    },{
				nil;
			    });

			    d[\charAfterAlphaToken] = if(d[\charAfterAlphaTokenPos].notNil,
				{ text[d[\charAfterAlphaTokenPos]]},
				nil
			    );

				//d[\typingAfterLastChar] = if (d[\charAfterAlphaToken].notNil,
					//{text[d[\currentLine]]}, nil);

			//////////////////////////////////////Numbers token//////////////////////////
					// find continuous block of numbers before current position

				 i = doc.selectionStart-1; //select only numbers Not letters
			    while( {
				if(i>=0,
						{not (text[i].isAlpha || text[i].isSpace || text[i].isPunct)},
				    false
				);
			    }, {
				i = i-1;
			    });

				d[\numbersBefore] = if( i == (doc.selectionStart-1),nil,text[(i+1)..(doc.selectionStart-1)]);
				d[\numbersBeforePos] = if(d[\numbersBefore].notNil,i+1,nil);//returns 0?
				//[d[\numbersBefore], d[\numbersBeforePos]].postln;


				// find continuous block of numbers after current position
			    i = doc.selectionStart;
			    while( {
				if(i<text.size,
						{not (text[i].isAlpha || text[i].isSpace || text[i].isPunct)}, //returns true
				    false
				);
			    }, {
				i = i+1;
			    });

			    d[\numbersAfter] = if( i == doc.selectionStart,nil,text[doc.selectionStart..(i-1)]);
			    d[\numbersAfterPos] = if(d[\numbersAfter].notNil,i-1,nil);

				d[\numberToken] = if(d[\numbersBefore].notNil,{
				d[\numbersBefore]++if(d[\numbersAfter].notNil,d[\numbersAfter],"");
			    },{
				if(d[\numbersAfter].notNil,d[\numbersAfter],nil);
			    });


				//////////////////////////char before number token
			    d[\charBeforeNumberTokenPos] = if(d[\numberToken].notNil,{
				if(d[\numbersBeforePos].notNil,
				    {if(d[\numbersBeforePos]>0,d[\numbersBeforePos]-1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart-1,nil)
				);
			    },{
				nil;
			    });

			    d[\charBeforeNumberToken] = if(d[\charBeforeNumberTokenPos].notNil,
					{text[d[\charBeforeNumberTokenPos]]},
					nil
			    );

			//////////////////////////char after number token

				d[\charAfterNumberTokenPos] = if(d[\numberToken].notNil,{
				if(d[\numbersAfterPos].notNil,
				    {if(d[\numbersAfterPos]>0,d[\numbersAfterPos]+1,nil)},
				    if(doc.selectionStart>0,doc.selectionStart+1,nil)
				);
			    },{
				nil;
			    });

			    d[\charAfterNumberToken] = if(d[\charAfterNumberTokenPos].notNil,
				{ text[d[\charAfterNumberTokenPos]]},
				nil
			    );

				////////////////////////////////////////////////////////////////////////
				d[\equalNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $\=, {d[\numberToken]},{nil});
			    },{
				nil;
			    });

				d[\equalAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\=, {d[\alphaToken]},{nil});
			    },{
				nil;
			    });

				d[\upper] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken].isUpper,d[\charBeforeAlphaToken]++d[\alphaToken],nil);
			    },{
				nil;
			    });

			    d[\symbol] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $\\,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a backslash at the beginning of the parameter


				d[\parenthesisNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $(, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we have a parenthesis at the beginning of the phrase

				d[\parenthesisAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $(,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a parenthesis at the beginning of the phrase

				d[\sqBracketNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $[, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we have a brackets at the beginning of the phrase

				d[\sqBracketAlpha] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == $[,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a brackets at the beginning of the phrase

				d[\function] = if(d[\alphaToken].notNil && d[\charBeforeAlphaToken].notNil,{
				if(d[\charBeforeAlphaToken] == ${,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a brackets at the beginning of the phrase

				d[\method] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\.,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a point at the beginning of the phrase


				d[\string] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $",d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when we have a string at the beginning of the phrase

				d[\commaNumber] = if(d[\numberToken].notNil && d[\charBeforeNumberToken].notNil,{
					if(d[\charBeforeNumberToken] == $,, {d[\numberToken]},{nil});
			    },{
				nil;
				});//recognize when we are in a line with commas (parameter line)

				d[\commaAlpha] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $\,,d[\alphaToken],nil);
			    },{
				nil;
				});//recognize when we are in a line with commas (parameter line)

				d[\tilde] = if(d[\alphaToken].notNil &&  d[\charBeforeAlphaToken].notNil,{
					if(d[\charBeforeAlphaToken] == $~,d[\alphaToken],nil);
			    },{
				nil;
				});//recognize when positioned in a global parameter (parameter line)


				d[\semicolon] = if(d[\alphaToken].notNil &&  d[\charAfterAlphaToken].notNil,{
					if(d[\charAfterAlphaToken] == $;,d[\alphaToken],nil);
			    },{
				nil;
			    });//recognize when I return to a previous idea

				//d[\charBeforeAlphaToken].postln;//
				//[d[\alphaToken],d[\alphasBeforePos],d[\charBeforeAlphaToken],  d[\symbol], d[\parenthesis], d[\sqBracket], d[\class], d[\tilde]].postln;
				//[d[\upper], d[\charBeforeAlphaToken]].postln;

				//[d[\alphaToken],d[\alphasAfterPos],d[\charAfterAlphaToken],   d[\semicolon]].postln;
				~keyData = ~keyData.add(d).keep(-80);

			    // ~keyData = ~keyData.add([char, Main.elapsedTime, ascii, modifier, keycode, doc.currentLine, /*posAffected*/]);
			};

			thisProcess.interpreter.codeDump = { |code|
			    // runs everytime code is evaluated
			    var d = Dictionary.new;
			    d[\code] = code;
			    d[\time] = Main.elapsedTime;
			    ~evalData = ~evalData.add(d);
			};

			/////////////////low level analysis//////////////////////////////////////

			~shortTypingFfilter = Array.new;
			~typingFfilter = Array.new;
			~enterKeysFfilter = Array.new;
			~arrowKeysFfilter = Array.new;
			~deletionFfilter = Array.new;
			~equalSignsFfilter= Array.new;
			~evaluationFfilter = Array.new;
			~parenthesisFfilter = Array.new;
			~sqBracketsFfilter= Array.new;
			~curlyBracketsFfilter= Array.new;
			~qMarksFfilter = Array.new;
			~smallLettersFfilter = Array.new;
			~capitalLettersFfilter = Array.new;
			~numbersFfilter = Array.new;
			~periodFfilter= Array.new;
			~commasFfilter =Array.new;
			~semicolonsFfilter= Array.new;
			~operatorsFfilter= Array.new;
			~inSymbolTypingFfilter  = Array.new;
			~inParenthesisTypingFfilter = Array.new;
			~inArrayFfilter= Array.new;
			~inFunctionFfilter= Array.new;
			~inMethodFfilter= Array.new;
			~inStringFfilter= Array.new;
			~inSemicolonFfilter= Array.new;
			~inGlobalVarFfilter= Array.new;
			~inStoringVarFfilter = Array.new;
			~typinginEndofDocFfilter= Array.new;
			~inNumbersFfilter= Array.new;
			~inClassFfilter= Array.new;
			~modifyingPrevStatementFfilter = Array.new;
			~inParametersTypingFfilter = Array.new;
			~typinginEmptyLineFfilter = Array.new;

			~samples = 2;
			~timeWindowSize = 5; //number of seconds back in time to look at typing data, to use with no normalized data
			~timeWindowSizeShort = 0.5; //to use in normalized typeFrequency
			~analysisPeriod = 0.05;
		
	Tdef (\lowlevelAnalysis, {inf.do
		{
			~now = Main.elapsedTime; // to make
		
			// another low level feature extractors...
			//typingFrequency:
		
			// frequency of keypresses in shorter moving window of last ~typingWindowSize seconds -> ~typingF
			~lastNSecondsShortTypingF = ~keyData.collect({|x|x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSizeShort)}) - ~now;
			~howManyShortTypingF = ~lastNSecondsShortTypingF.size;
			~shortTypingFraw = ~howManyShortTypingF/~timeWindowSizeShort;
			~shortTypingFfilter = ~shortTypingFfilter.insert(0,~shortTypingFraw); //inserting frequencies
			~shortTypingFfilter = ~shortTypingFfilter.keep(~samples);
			~shortTypingFsmooth = ~shortTypingFfilter.mean; //freq average
			~shortTypingF = (~shortTypingFsmooth/24).clip(0,1);
			{~shortTypingFInfo.value= ~shortTypingF}.defer;
		
			// frequency of keypresses in longer moving window of last ~typingWindowSize seconds -> ~typingF
		
			~lastNSecondsTypingF = ~keyData.collect({|x|x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howMany = ~lastNSecondsTypingF .size;
			~typingFraw = ~howMany/~timeWindowSize;
			~typingFfilter = ~typingFfilter.insert(0,~typingFraw);
			~typingFfilter = ~typingFfilter.keep(~samples);
			~typingFsmooth = ~typingFfilter.mean;
			~typingF = (~typingFsmooth/24).clip(0,1);
			{~typingFInfo.value= ~typingF}.defer;
			//~typingF.postln;
		
			//"enterKey" sign frecuency by extracting ascii 13
		
			~lastNSecondsenterKeysF = ~keyData.collect({|x| if (x[\ascii] == 13, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyenterKeys = ~lastNSecondsenterKeysF.size;
			if (~typingF == 0, {~enterKeysFraw = 0}, {~enterKeysFraw = ~howManyenterKeys/~typingFsmooth});
			~enterKeysFfilter = ~enterKeysFfilter.insert(0,~enterKeysFraw);
			~enterKeysFfilter = ~enterKeysFfilter.keep(~samples);
			~enterKeysFsmooth = ~enterKeysFfilter.mean;
			~enterKeysF = ~enterKeysFsmooth;
			{~enterKeysFInfo.value = ~enterKeysF}.defer;
		
		
			//"arrowKeys" sign frecuency by extracting keycode 65361, 65362, 65363, 65364
		
			~lastNSecondsarrowKeysF = ~keyData.collect({|x| if ((x[\keycode] == 65361) || (x[\keycode] == 65362) || (x[\keycode] == 65363) || (x[\keycode] == 65363), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyarrowKeys = ~lastNSecondsarrowKeysF.size;
			if (~typingF == 0, {~arrowKeysFraw = 0}, {~arrowKeysFraw = ~howManyarrowKeys/~typingFsmooth});
			~arrowKeysFfilter = ~arrowKeysFfilter.insert(0,~arrowKeysFraw);
			~arrowKeysFfilter = ~arrowKeysFfilter.keep(~samples);
			~arrowKeysFsmooth = ~arrowKeysFfilter.mean;
			~arrowKeysF = ~arrowKeysFsmooth;
			{~arrowKeysFInfo.value = ~arrowKeysF}.defer;
			//equal sign frecuency by extracting ascii 61 "="
		
			~lastNSecondsequalSignsF = ~keyData.collect({|x| if (x[\ascii] == 61, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyequalSigns = ~lastNSecondsequalSignsF.size;
			if (~typingF == 0, {~equalSignsFraw = 0}, {~equalSignsFraw = ~howManyequalSigns/~typingFsmooth});
			~equalSignsFfilter = ~equalSignsFfilter.insert(0,~equalSignsFraw);
			~equalSignsFfilter = ~equalSignsFfilter.keep(~samples);
			~equalSignsFsmooth = ~equalSignsFfilter.mean;
			~equalSignsF = ~equalSignsFsmooth;
			{~equalSignsFInfo.value = ~equalSignsF}.defer;
		
		
			//deletion frecuency by extracting ascii 8 & 127 (backspace and supr)
		
			~lastNSecondsDelF = ~keyData.collect({|x| if ((x[\ascii] == 8) || (x[\ascii] == 127), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyDeletes = ~lastNSecondsDelF.size;
			if (~typingF == 0, {~deletionFraw = 0}, {~deletionFraw = ~howManyDeletes/~typingFsmooth});
			~deletionFfilter = ~deletionFfilter.insert(0,~deletionFraw);
			~deletionFfilter = ~deletionFfilter.keep(~samples);
			~deletionFsmooth = ~deletionFfilter.mean;
			~deletionF = (~deletionFsmooth/24).clip(0,1);
			{~deletionFInfo.value = ~deletionF}.defer;
		
			//evaluation Frequency
		
			~lastNSecondsevaluationF = ~evalData.collect({|x| x[\time]}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyevaluations = ~lastNSecondsevaluationF.size;
			if (~typingF == 0, {~evaluationFraw = 0}, {~evaluationFraw = ~howManyevaluations/~typingFsmooth});
			~evaluationFfilter = ~evaluationFfilter.insert(0,~evaluationFraw);
			~evaluationFfilter= ~evaluationFfilter.keep(~samples);
			~evaluationFsmooth= ~evaluationFfilter.mean;
			~evaluationF = (~evaluationFsmooth/24).clip(0,1);
			{~evaluationFInfo.value =  ~evaluationF}.defer;
		
		
			//number of parenthesis in last n seconds in Hz by extracting ascii code 40 & 41
		
			~lastNSecondsParenthesisF = ~keyData.collect({|x| if ((x[\ascii] == 40) || (x[\ascii] == 41), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
		
			~howManyParenthesis = ~lastNSecondsParenthesisF.size;
			if (~typingF == 0, {~parenthesisFraw = 0}, {~parenthesisFraw = ~howManyParenthesis/~typingFsmooth});
			~parenthesisFfilter = ~parenthesisFfilter.insert(0,~parenthesisFraw);
			~parenthesisFfilter= ~parenthesisFfilter.keep(~samples);
			~parenthesisFsmooth= ~parenthesisFfilter.mean;
			~parenthesisF= ~parenthesisFsmooth;
			{~parenthesisFInfo.value =  ~parenthesisF}.defer;
		
			//number of square brackets in last n seconds in Hz by extracting ascii code 91 & 93
		
			~lastNSecondsSqBracketsF= ~keyData.collect({|x| if ((x[\ascii] == 91) || (x[\ascii] == 93), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManySqBrackets= ~lastNSecondsSqBracketsF.size;
			if (~typingF == 0, {~sqBracketsFraw = 0}, {~sqBracketsFraw = ~howManySqBrackets/~typingFsmooth});
			~sqBracketsFfilter = ~sqBracketsFfilter.insert(0,~sqBracketsFraw);
			~sqBracketsFfilter= ~sqBracketsFfilter.keep(~samples);
			~sqBracketsFsmooth= ~sqBracketsFfilter.mean;
			~sqBracketsF= ~sqBracketsFsmooth;
			{~sqBracketsFInfo.value =  ~sqBracketsF}.defer;
		
		
			//number of curly brackets in last n seconds in Hz by extracting ascii code 123 & 125
		
			~lastNSecondsCurlyBracketsF= ~keyData.collect({|x| if ((x[\ascii] == 123) || (x[\ascii] == 125), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyCurlyBrackets= ~lastNSecondsCurlyBracketsF.size;
			if (~typingF == 0, {~curlyBracketsFraw = 0}, {~curlyBracketsFraw = ~howManyCurlyBrackets/~typingFsmooth});//limiting to zero
			~curlyBracketsFfilter = ~curlyBracketsFfilter.insert(0,~curlyBracketsFraw); //inserting frequencies
			~curlyBracketsFfilter= ~curlyBracketsFfilter.keep(~samples);
			~curlyBracketsFsmooth= ~curlyBracketsFfilter.mean;
			~curlyBracketsF= ~curlyBracketsFsmooth;
			{~curlyBracketsFInfo.value =  ~curlyBracketsF}.defer;//posting data in window
		
			//number of quotation mark in last n seconds (Hz) by extracting ascii code 34
		
			~lastNSecondsQMarksF= ~keyData.collect({|x| if (x[\ascii] == 34, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyqMarks= ~lastNSecondsQMarksF.size;
			if (~typingF == 0, {~qMarksFraw = 0}, {~qMarksFraw = ~howManyqMarks/~typingFsmooth});//limiting to zero
			~qMarksFfilter = ~qMarksFfilter.insert(0,~qMarksFraw); //inserting deletion frequencies
			~qMarksFfilter= ~qMarksFfilter.keep(~samples);
			~qMarksFsmooth= ~qMarksFfilter.mean;
			~qMarksF= ~qMarksFsmooth/24;
			{~qMarksFInfo.value =  ~qMarksF}.defer;//posting data in window
		
			//number of lower case letters in last n seconds (Hz) by extracting ascii code 97-122
		
			~lastNSecondssmallLettersF= ~keyData.collect({|x| if ((x[\ascii] >= 97) && (x[\ascii] <= 122), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManysmallLetters= ~lastNSecondssmallLettersF.size;
			if (~typingF == 0, {~smallLettersFraw = 0}, {~smallLettersFraw = ~howManysmallLetters/~typingFsmooth});//limiting to zero
			~smallLettersFfilter = ~smallLettersFfilter.insert(0,~smallLettersFraw); //inserting frequencies
			~smallLettersFfilter= ~smallLettersFfilter.keep(~samples);
			~smallLettersFsmooth= ~smallLettersFfilter.mean;
			~smallLettersF= ~smallLettersFsmooth;
			{~smallLettersFInfo.value =  ~smallLettersF}.defer;//posting data in window
		
		
			//number of capital letters in last n seconds (Hz) by extracting ascii code 65-90
		
			~lastNSecondscapitalLettersF= ~keyData.collect({|x| if ((x[\ascii] >= 65) && (x[\ascii] <= 90), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManycapitalLetters= ~lastNSecondscapitalLettersF.size;
			if (~typingF == 0, {~capitalLettersFraw = 0}, {~capitalLettersFraw = ~howManycapitalLetters/~typingFsmooth});//limiting to zero
			~capitalLettersFfilter = ~capitalLettersFfilter.insert(0,~capitalLettersFraw); //inserting frequencies
			~capitalLettersFfilter= ~capitalLettersFfilter.keep(~samples);
			~capitalLettersFsmooth= ~capitalLettersFfilter.mean;
			~capitalLettersF= ~capitalLettersFsmooth;
			{~capitalLettersFInfo.value =  ~capitalLettersF}.defer;//posting data in window
		
		
			//number of numbers in last n seconds (Hz) by extracting ascii code 48-57
		
			~lastNSecondsnumbersF= ~keyData.collect({|x| if ((x[\ascii] >= 48) && (x[\ascii] <= 57), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManynumbers= ~lastNSecondsnumbersF.size;
			if (~typingF == 0, {~numbersFraw = 0}, {~numbersFraw = ~howManynumbers/~typingFsmooth});//limiting to zero
			~numbersFfilter = ~numbersFfilter.insert(0,~numbersFraw); //inserting frequencies
			~numbersFfilter= ~numbersFfilter.keep(~samples);
			~numbersFsmooth= ~numbersFfilter.mean;
			~numbersF= ~numbersFsmooth;
			{~numbersFInfo.value =  ~numbersF}.defer;//posting data in window
		
			//number of period in last n seconds (Hz) by extracting ascii code 48-57
		
			~lastNSecondsperiodF= ~keyData.collect({|x| if (x[\ascii] == 46, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyperiod= ~lastNSecondsperiodF.size;
			if (~typingF == 0, {~periodFraw = 0}, {~periodFraw = ~howManyperiod/~typingFsmooth});//limiting to zero
			~periodFfilter = ~periodFfilter.insert(0,~periodFraw); //inserting frequencies
			~periodFfilter= ~periodFfilter.keep(~samples);
			~periodFsmooth= ~periodFfilter.mean;
			~periodF= ~periodFsmooth;
			{~periodFInfo.value =  ~periodF}.defer;//posting data in window
		
			//number of commas in last n seconds (Hz) by extracting ascii code 44
		
			~lastNSecondscommasF= ~keyData.collect({|x| if (x[\ascii] == 44, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManycommas= ~lastNSecondscommasF.size;
			if (~typingF == 0, {~commasFraw = 0}, {~commasFraw = ~howManycommas/~typingFsmooth});//limiting to zero
			~commasFfilter = ~commasFfilter.insert(0,~commasFraw); //inserting frequencies
			~commasFfilter= ~commasFfilter.keep(~samples);
			~commasFsmooth= ~commasFfilter.mean;
			~commasF= ~commasFsmooth;
			{~commasFInfo.value =  ~commasF}.defer;//posting data in window
		
			//number of semicolons in last n seconds (Hz) by extracting ascii code 59
		
			~lastNSecondssemicolonsF= ~keyData.collect({|x| if (x[\ascii] == 59, {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManysemicolons= ~lastNSecondssemicolonsF.size;
			if (~typingF == 0, {~semicolonsFraw = 0}, {~semicolonsFraw = ~howManysemicolons/~typingFsmooth});//limiting to zero
			~semicolonsFfilter = ~semicolonsFfilter.insert(0,~semicolonsFraw); //inserting frequencies
			~semicolonsFfilter= ~semicolonsFfilter.keep(~samples);
			~semicolonsFsmooth= ~semicolonsFfilter.mean;
			~semicolonsF= ~semicolonsFsmooth;
			{~semicolonsFInfo.value =  ~semicolonsF}.defer;//posting data in window
		
			//number of basic operators (*,+,-, /) in last n seconds (Hz) by extracting ascii code 42,43,45,47
		
			~lastNSecondsoperatorsF=  ~keyData.collect({|x| if ((x[\ascii] == 42) || (x[\ascii] == 43) || (x[\ascii] == 45) ||(x[\ascii] == 47), {x[\time]}, {0})}).removeAllSuchThat({|x|x>=(~now-~timeWindowSize)}) - ~now;
			~howManyoperators= ~lastNSecondsoperatorsF.size;
			if (~typingF == 0, {~operatorsFraw = 0}, {~operatorsFraw = ~howManyoperators/~typingFsmooth});//limiting to zero
			~operatorsFfilter = ~operatorsFfilter.insert(0,~operatorsFraw); //inserting frequencies
			~operatorsFfilter= ~operatorsFfilter.keep(~samples);
			~operatorsFsmooth= ~operatorsFfilter.mean;
			~operatorsF= ~operatorsFsmooth;
			{~operatorsFInfo.value =  ~operatorsF}.defer;//posting data in window
		
		
			///////////////////////////////lexical analysis///////////////////
			//in Parameters:
		
			// in "symbol" typing
			~lastNSecondsinSymbolTypingF = ~keyData.collect ({|d| if (d[\symbol].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinSymbolTypingF= ~lastNSecondsinSymbolTypingF.size;
			if (~typingF == 0, {~inSymbolTypingFraw = 0}, {~inSymbolTypingFraw = ~howManyinSymbolTypingF/~typingFsmooth});//limiting to zero
			~inSymbolTypingFfilter = ~inSymbolTypingFfilter.insert(0,~inSymbolTypingFraw);
			~inSymbolTypingFfilter= ~inSymbolTypingFfilter.keep(~samples);
			~inSymbolTypingFsmooth= ~inSymbolTypingFfilter.mean;
			~inSymbolTypingF= ~inSymbolTypingFsmooth;
			{~inSymbolTypingFInfo.value =  ~inSymbolTypingF}.defer;//posting data in window
		
		
			// in "parameters" typing
			~lastNSecondsinParametersTypingF = ~keyData.collect ({|d| if ((d[\commaNumber].notNil) || (d[\commaAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinParametersTypingF= ~lastNSecondsinParametersTypingF.size;
			if (~typingF == 0, {~inParametersTypingFraw = 0}, {~inParametersTypingFraw = ~howManyinParametersTypingF/~typingFsmooth});//limiting to zero
			~inParametersTypingFfilter = ~inParametersTypingFfilter.insert(0,~inParametersTypingFraw);
			~inParametersTypingFfilter= ~inParametersTypingFfilter.keep(~samples);
			~inParametersTypingFsmooth= ~inParametersTypingFfilter.mean;
			~inParametersTypingF= ~inParametersTypingFsmooth;
			{~inParametersTypingFInfo.value =  ~inParametersTypingF}.defer;//posting data in window
		
			// in "parenthesis" typing
			~lastNSecondsinParenthesisTypingF = ~keyData.collect ({|d| if ((d[\parenthesisNumber].notNil)|| (d[\parenthesisAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinParenthesisTypingF= ~lastNSecondsinParenthesisTypingF.size;
			if (~typingF == 0, {~inParenthesisTypingFraw = 0}, {~inParenthesisTypingFraw = ~howManyinParenthesisTypingF/~typingFsmooth});//limiting to zero
			~inParenthesisTypingFfilter = ~inParenthesisTypingFfilter.insert(0,~inParenthesisTypingFraw);
			~inParenthesisTypingFfilter= ~inParenthesisTypingFfilter.keep(~samples);
			~inParenthesisTypingFsmooth= ~inParenthesisTypingFfilter.mean;
			~inParenthesisTypingF= ~inParenthesisTypingFsmooth;
			{~inParenthesisTypingFInfo.value =  ~inParenthesisTypingF}.defer;//posting data in window
		
			// in "Array/List" typing
			~lastNSecondsinArrayF = ~keyData.collect ({|d| if ((d[\sqBracketNumber].notNil) || (d[\sqBracketAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinArrayF= ~lastNSecondsinArrayF.size;
			if (~typingF == 0, {~inArrayFraw = 0}, {~inArrayFraw = ~howManyinArrayF/~typingFsmooth});//limiting to zero
			~inArrayFfilter = ~inArrayFfilter.insert(0,~inArrayFraw);
			~inArrayFfilter= ~inArrayFfilter.keep(~samples);
			~inArrayFsmooth= ~inArrayFfilter.mean;
			~inArrayF= ~inArrayFsmooth;
			{~inArrayFInfo.value =  ~inArrayF}.defer;//posting data in window
		
		
			//in token of "numbers"
		
			~lastNSecondsinNumbersF = ~keyData.collect ({|d| if (d[\numberToken].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinNumbersF= ~lastNSecondsinNumbersF.size;
			if (~typingF == 0, {~inNumbersFraw = 0}, {~inNumbersFraw = ~howManyinNumbersF/~typingFsmooth});//limiting to zero
			~inNumbersFfilter = ~inNumbersFfilter.insert(0,~inNumbersFraw);
			~inNumbersFfilter= ~inNumbersFfilter.keep(~samples);
			~inNumbersFsmooth= ~inNumbersFfilter.mean;
			~inNumbersF= ~inNumbersFsmooth;
			{~inNumbersFInfo.value =  ~inNumbersF}.defer;//posting data in window
		
			//in "function" typing
		
			~lastNSecondsinFunctionF = ~keyData.collect ({|d| if (d[\function].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinFunctionF= ~lastNSecondsinFunctionF.size;
			if (~typingF == 0, {~inFunctionFraw = 0}, {~inFunctionFraw = ~howManyinFunctionF/~typingFsmooth});//limiting to zero
			~inFunctionFfilter = ~inFunctionFfilter.insert(0,~inFunctionFraw);
			~inFunctionFfilter= ~inFunctionFfilter.keep(~samples);
			~inFunctionFsmooth= ~inFunctionFfilter.mean;
			~inFunctionF= ~inFunctionFsmooth;
			{~inFunctionFInfo.value =  ~inFunctionF}.defer;
		
		
			//in "method" typing by mapping period
		
			~lastNSecondsinMethodF = ~keyData.collect ({|d| if (d[\method].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinMethodF= ~lastNSecondsinMethodF.size;
			if (~typingF == 0, {~inMethodFraw = 0}, {~inMethodFraw = ~howManyinMethodF/~typingFsmooth});//limiting to zero
			~inMethodFfilter = ~inMethodFfilter.insert(0,~inMethodFraw);
			~inMethodFfilter= ~inMethodFfilter.keep(~samples);
			~inMethodFsmooth= ~inMethodFfilter.mean;
			~inMethodF= ~inMethodFsmooth;
			{~inMethodFInfo.value =  ~inMethodF}.defer;
		
			//in "string" typing
		
			~lastNSecondsinStringF = ~keyData.collect ({|d| if (d[\string].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinStringF= ~lastNSecondsinStringF.size;
			if (~typingF == 0, {~inStringFraw = 0}, {~inStringFraw = ~howManyinStringF/~typingFsmooth});//limiting to zero
			~inStringFfilter = ~inStringFfilter.insert(0,~inStringFraw);
			~inStringFfilter= ~inStringFfilter.keep(~samples);
			~inStringFsmooth= ~inStringFfilter.mean;
			~inStringF= ~inStringFsmooth;
			{~inStringFInfo.value =  ~inStringF}.defer;
		
			// NewStatement: adding structural information
		
			//in "class" typing
		
			~lastNSecondsinClassF = ~keyData.collect ({|d| if (d[\upper].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinClassF= ~lastNSecondsinClassF.size;
			if (~typingF == 0, {~inClassFraw = 0}, {~inClassFraw = ~howManyinClassF/~typingFsmooth});//limiting to zero
			~inClassFfilter = ~inClassFfilter.insert(0,~inClassFraw);
			~inClassFfilter= ~inClassFfilter.keep(~samples);
			~inClassFsmooth= ~inClassFfilter.mean;
			~inClassF= ~inClassFsmooth;
			{~inClassFInfo.value =  ~inClassF}.defer;
		
			//typing in "end of doc". Typing at the end of the document.
		
			~lastNSecondsTypinginEndofDocF = ~keyData.collect ({|d|if (d[\endOfDocPos].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyTypinginEndofDocF= ~lastNSecondsTypinginEndofDocF.size;
		
			if (~typingF == 0, {~typinginEndofDocFraw = 0}, {~typinginEndofDocFraw = ~howManyTypinginEndofDocF/~typingFsmooth});//limiting to zero
		
			~typinginEndofDocFfilter = ~typinginEndofDocFfilter.insert(0,~typinginEndofDocFraw);
			~typinginEndofDocFfilter= ~typinginEndofDocFfilter.keep(~samples);
			~typinginEndofDocFsmooth= ~typinginEndofDocFfilter.mean;
			~typinginEndofDocF= ~typinginEndofDocFsmooth;
			{~typinginEndofDocFInfo.value =  ~typinginEndofDocF}.defer;
		
			//typing in "previous empty line", adding new info in past "empty lines"
		
			~lastNSecondstypinginEmptyLineF = ~keyData.collect ({|d|if (d[\posInEmptyLine].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManytypinginEmptyLineF= ~lastNSecondstypinginEmptyLineF.size;
		
			if (~typingF == 0, {~typinginEmptyLineFraw = 0}, {~typinginEmptyLineFraw = ~howManytypinginEmptyLineF/~typingFsmooth});//limiting to zero
		
			~typinginEmptyLineFfilter = ~typinginEmptyLineFfilter.insert(0,~typinginEmptyLineFraw);
			~typinginEmptyLineFfilter= ~typinginEmptyLineFfilter.keep(~samples);
			~typinginEmptyLineFsmooth= ~typinginEmptyLineFfilter.mean;
			~typinginEmptyLineF= ~typinginEmptyLineFsmooth;
			{~typinginEmptyLineFInfo.value =  ~typinginEmptyLineF}.defer;
		
		
			//in "global var"
			~lastNSecondsinGlobalVarF = ~keyData.collect ({|d| if (d[\tilde].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinGlobalVarF= ~lastNSecondsinGlobalVarF.size;
			if (~typingF == 0, {~inGlobalVarFraw = 0}, {~inGlobalVarFraw = ~howManyinGlobalVarF/~typingFsmooth});//limiting to zero
			~inGlobalVarFfilter = ~inGlobalVarFfilter.insert(0,~inGlobalVarFraw);
			~inGlobalVarFfilter= ~inGlobalVarFfilter.keep(~samples);
			~inGlobalVarFsmooth= ~inGlobalVarFfilter.mean;
			~inGlobalVarF= ~inGlobalVarFsmooth;
			{~inGlobalVarFInfo.value =  ~inGlobalVarF}.defer;
		
		
			//in "storing variable" by extracting alpha token after "="
			~lastNSecondsinStoringVarF = ~keyData.collect ({|d| if ((d[\equalNumber].notNil) || (d[\equalAlpha].notNil), {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinStoringVarF= ~lastNSecondsinStoringVarF.size;
			if (~typingF == 0, {~inStoringVarFraw = 0}, {~inStoringVarFraw = ~howManyinStoringVarF/~typingFsmooth});//limiting to zero
			~inStoringVarFfilter = ~inStoringVarFfilter.insert(0,~inStoringVarFraw);
			~inStoringVarFfilter= ~inStoringVarFfilter.keep(~samples);
			~inStoringVarFsmooth= ~inStoringVarFfilter.mean;
			~inStoringVarF= ~inStoringVarFsmooth;
			{~inStoringVarFInfo.value =  ~inStoringVarF}.defer;
		
		
			////////////changes///////
			//in line with a "semicolon"
		
			~lastNSecondsinSemicolonF = ~keyData.collect ({|d| if (d[\semicolon].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManyinSemicolonF= ~lastNSecondsinSemicolonF.size;
			if (~typingF == 0, {~inSemicolonFraw = 0}, {~inSemicolonFraw = ~howManyinSemicolonF/~typingFsmooth});//limiting to zero
			~inSemicolonFfilter = ~inSemicolonFfilter.insert(0,~inSemicolonFraw);
			~inSemicolonFfilter= ~inSemicolonFfilter.keep(~samples);
			~inSemicolonFsmooth= ~inSemicolonFfilter.mean;
			~inSemicolonF= ~inSemicolonFsmooth;
			{~inSemicolonFInfo.value =  ~inSemicolonF}.defer;
		
		
			//modifying "previous (written) statement"
		
			~lastNSecondsmodifyingPrevStatementF = ~keyData.collect ({|d|if (d[\posInDoc].notNil, {d[\time]}, {0})}).removeAllSuchThat({|t|t>=(~now-~timeWindowSize)}) - ~now;
			~howManymodifyingPrevStatementF= ~lastNSecondsmodifyingPrevStatementF.size;
			if (~typingF == 0, {~modifyingPrevStatementFraw = 0}, {~modifyingPrevStatementFraw = ~howManymodifyingPrevStatementF/~typingFsmooth});//limiting to zero
		
			~modifyingPrevStatementFfilter = ~modifyingPrevStatementFfilter.insert(0,~modifyingPrevStatementFraw);
			~modifyingPrevStatementFfilter= ~modifyingPrevStatementFfilter.keep(~samples);
			~modifyingPrevStatementFsmooth= ~modifyingPrevStatementFfilter.mean;
			~modifyingPrevStatementF= ~modifyingPrevStatementFsmooth;
			{~modifyingPrevStatementInfo.value =  ~modifyingPrevStatementF}.defer;
		
		

			if (allowBroadcast, {
			broadcast.sendMsg("/coding/typing/typingF",~shortTypingF);
			broadcast.sendMsg("/coding/typing/deletionF",~deletionF);
			broadcast.sendMsg("/coding/typing/evaluationF",~evaluationF);
			/////////////////sending data to wekinator//////
			wek.sendMsg("/wek/inputs", ~enterKeysF, ~arrowKeysF, ~equalSignsF,  ~parenthesisF, ~sqBracketsF, ~curlyBracketsF, ~qMarksF, ~smallLettersF, ~capitalLettersF,  ~numbersF, ~periodF, ~commasF, ~semicolonsF, ~operatorsF, ~inSymbolTypingF, ~inParametersTypingF, ~inParenthesisTypingF, ~inArrayF, ~inNumbersF, ~inFunctionF, ~inMethodF, ~inStringF, ~inClassF, ~inGlobalVarF, ~inStoringVarF, ~inSemicolonF, ~typinginEndofDocF, ~typinginEmptyLineF, ~modifyingPrevStatementF );
		
});


				// wait to do it again...
				~analysisPeriod.wait;
			    }
			}).play;
	}
}
