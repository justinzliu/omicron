package com.omicron.android.cmpt276_1191e1_omicron;

import android.content.Intent;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;


public class MainActivity extends AppCompatActivity
{
	/*
	 *  This Main Activity is the activity that will act as the Start Menu
	 */

	RadioGroup Difficulty;
	RadioGroup Language;
	RadioGroup Mode;
	private int usrModePref = 0; // 0=standard, 1=speech
	private int usrLangPref = 0; // 0=eng_fr, 1=fr_eng; 0 == native(squares that cannot be modified); 1 == translation(the words that the user inserts)
	private int usrDiffPref; //0=easy,1=medium,2=difficult
	private int state = 0; //0=new start, 1=resume
	private String language;
	private boolean canStart = true;
	private int[] usrPuzzleTypePref = { -1 }; //determines if it is a 4x4, 6x6, 9x9 or 12x12 sudoku puzzle
	private RadioGroup pkgRadioGroup;
	
	WordArray wordArrayResume;
	String[] numArrayResume;
	private int usrLangPrefResume;
	private SudokuGenerator usrSudokuArrResume;
	int usrModePrefResume;
	String languageResume;

	//used only for user entering language check//convert to appropriate tag
	private TextToSpeech lTTS;
	Locale[] thelocale;
	private List<String> lTTSlanguage = new ArrayList<String>();
	private List<String> lTTScountry = new ArrayList<String>();
	private List<String> lTTSlangTags = new ArrayList<String>();
	private List<Locale> localeList = new ArrayList<Locale>();

	private WordPackageFileIndex wordPackageFileIndexArr; //stores word packages name and internal file name
	private int MAX_WORD_PKG = 50; //max word packages user is allowed to import
	private int MAX_CSV_ROW = 150; //allow up to 150 pairs per package; IMPORTANT: because of WordArray.selectWord(), too many words may cause an error
	private int MIN_CSV_ROW = 12; //minimum number of words required in file
	private int CURRENT_WORD_PKG_COUNT = 0; //stores current number of packages the user has uploaded
	private int HINT_CLICK_TO_MAX_PROB = 15; //defines how many HintClicks are required for a word to reach MAX_WORD_UNIT_LIMIT
	private FileCSV fileCSV; //object containing CSV functions
	private int[] indexOfRadBtnToRemove = { -1 }; //which radio btn to remove
	private boolean[] removeBtnEnable = { true }; //when false, do not allow "REMOVE PKG" button (required because GameActivity may be using that file to save "Hint Click")
	
	private WordArray wordArray;
	/*private Word[] wordArray =new Word[]
			{
					new Word( "Un", "Un", 1, 1 ),
					new Word( "Two", "Deux", 2, 1 ),
					new Word( "Three", "Trois", 3, 1 ),
					new Word( "Four", "Quatre", 4, 1 ),
					new Word( "Five", "Cinq", 5, 1 ),
					new Word( "Six", "Six", 6, 1 ),
					new Word( "Seven", "Sept", 7, 1 ),
					new Word( "Eight", "Huit", 8, 1 ),
					new Word( "Nine", "Neuf", 9, 1 ),
					new Word( "en-US", "fr-FR", -1, -1 ), //lang
					new Word( "pkg_n.csv", "", -1, -1 ) //pkg name
			};*/
	
	
	
	// TODO: fix buttons in landscape mode
	
	// TODO: separate all of Intent activity.putExtra( ) outside of MainActivity in different functions
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		if (savedInstanceState != null) {
			//a state had been saved, load it. if state == 0, there is nothing to load.
			state = (int) savedInstanceState.getSerializable("state");
			if (state == 1) {
				wordArrayResume = (WordArray) savedInstanceState.getParcelable("wordArrayMA");
				usrLangPrefResume = savedInstanceState.getInt("usrLangPrefMA");
				usrSudokuArrResume = (SudokuGenerator) savedInstanceState.get("SudokuArrMA");
				usrModePrefResume = (int) savedInstanceState.getSerializable("usrModeMA");
				languageResume = (String) savedInstanceState.getSerializable("languageMA");
				if (usrModePrefResume == 1) {
					numArrayResume = (String[]) savedInstanceState.getSerializable("numArrayMA");
				}
			}
		}
		
		fileCSV = new FileCSV( MAX_WORD_PKG, MAX_CSV_ROW, MIN_CSV_ROW );
		
		pkgRadioGroup = findViewById( R.id.pkg_radio_group ); //stores all the radio buttons with file names
		
		int res = checkIfJustInstalledAndSetUpPackagesAlreadyInstalled( );
		
		if( res != 0 ) //some exception occurred
		{ return; }
		
		
		// SET LISTENERS TO WHICH PKG IS SELECTED //
		
		pkgRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int pkgSelectId = group.getCheckedRadioButtonId();
				switch(pkgSelectId)
				{
					//void
				}
			}
		});
		
		
		//create button which will start new UploadActivity to upload and process .csv file
		Button btn_upload = findViewById( R.id.btn_upload );
		btn_upload.setOnClickListener(new View.OnClickListener( )
								{
									@Override
									public void onClick( View v )
									{
										
										Intent uploadActivityIntent = new Intent( MainActivity.this, UploadActivity.class );
										uploadActivityIntent.putExtra( "MAX_WORD_PKG", MAX_WORD_PKG );
										uploadActivityIntent.putExtra( "MAX_CSV_ROW", MAX_CSV_ROW );
										uploadActivityIntent.putExtra( "MIN_CSV_ROW", MIN_CSV_ROW );
										
										startActivity( uploadActivityIntent );
									}
								}
		);
		
		
		//create button which will remove file
		final Button btn_remove = findViewById( R.id.btn_remove );
		btn_remove.setOnClickListener(new View.OnClickListener( )
									  {
										  @Override
										  public void onClick( View v )
										  {
											  //FIND WHICH BUTTON IS SELECTED
											  int indexOfRadBtnToRemove2 = pkgRadioGroup.indexOfChild( findViewById( (pkgRadioGroup.getCheckedRadioButtonId()) ) );
											  indexOfRadBtnToRemove[0] = indexOfRadBtnToRemove2; //save a local copy needed for onStop() to remove radio btn
											  
											  String pkgInternalFileName = wordPackageFileIndexArr.getPackageFileAtIndex( indexOfRadBtnToRemove2 ).getInternalFileName( ); //find internal file name of pkg to remove
											  String pkgName = wordPackageFileIndexArr.getPackageFileAtIndex( indexOfRadBtnToRemove2 ).getWordPackageName( ); //user defined pkg name
											  
											  Intent removeActivityIntent = new Intent( MainActivity.this, RemoveActivity.class );
											  removeActivityIntent.putExtra( "indexOfRadBtnToRemove", indexOfRadBtnToRemove2 );
											  removeActivityIntent.putExtra( "pkgInternalFileName", pkgInternalFileName );
											  removeActivityIntent.putExtra( "pkgName", pkgName );
											
											  startActivity( removeActivityIntent );
										  }
									  }
		);
		
		
		
			// CHOOSE THE LEVEL OF DIFFICULTY
		Difficulty = findViewById(R.id.button_level);
		Difficulty.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int difficultyId = group.getCheckedRadioButtonId();
				switch(difficultyId)
				{
					case R.id.button_easy:
						usrDiffPref = 0;
						break;

					case R.id.button_medium:
						usrDiffPref = 1;
						break;

					case R.id.button_hard:
						usrDiffPref = 2;
						break;
				}
			}
		});


			// CHOOSE THE LANGUAGE
		Language=findViewById(R.id.button_language);
		Language.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int LanguageId = group.getCheckedRadioButtonId();
				switch (LanguageId)
				{
					case R.id.button_eng_fr:
						usrLangPref = 0;
						break;

					case R.id.button_fr_eng:
						usrLangPref = 1;
						break;
				}
			}
		});

		// CHOOSE THE MODE
		Mode=findViewById(R.id.button_mode);
		Mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int ModeId = group.getCheckedRadioButtonId();
				switch (ModeId)
				{
					case R.id.button_mStandard:
						usrModePref = 0;
						break;

					case R.id.button_mSpeech:
						usrModePref = 1;
						break;
				}
			}
		});

		lTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.SUCCESS) {
					thelocale = Locale.getAvailableLocales();
					//For optional implementations
					String TTSlanguage;
					String TTScountry;
					int counter = 0;
					for (Locale LO : thelocale) {
						int res = lTTS.isLanguageAvailable(LO);
						if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
							//store all available locales as Locale type
							localeList.add(LO);
							//store all available language tag (String)
							lTTSlangTags.add(LO.toLanguageTag());
							//Log.e("lTTS", "LanguageTag is: "+lTTSlangTags.get(counter));
							//For optional implementations
							//store all available locales in language - country format (strings)
							TTSlanguage = LO.getDisplayLanguage();
							lTTSlanguage.add(TTSlanguage);
							TTScountry = LO.getDisplayCountry();
							lTTScountry.add(TTScountry);
							//Log.e("lTTS", "Language and Country is: "+lTTSlanguage.get(counter)+" - "+lTTScountry.get(counter));
							counter++;
						}
					}
				}
				else {
					Log.e("lTTS", "TTS failed to initiate");
				}
			}
		});

		
			/** START GAME BUTTON **/
		
		// used to switch to gameActivity
		Button btnStart = (Button) findViewById( R.id.button_start );
		btnStart.setOnClickListener( new View.OnClickListener(  )
			{
				@Override
				public void onClick( View v )
				{
					RadioButton radBtnSelected = findViewById( pkgRadioGroup.getCheckedRadioButtonId() );
					String fileNameSelected = wordPackageFileIndexArr.getPackageFileAtIndex( pkgRadioGroup.indexOfChild( radBtnSelected ) ).getInternalFileName( ); //get pkg internal file name to find csv
					
					//initialize a word array to store puzzle words and preferences
					RadioGroup radGroup = findViewById( R.id.btn_type );
					usrPuzzleTypePref[0] = findUserPuzzleTypePreference( radGroup ); //stores user puzzle preference inside wordArray
					wordArray = new WordArray( usrPuzzleTypePref[0], MAX_CSV_ROW, HINT_CLICK_TO_MAX_PROB );
					
					try {
						//based on pkg, initialize the wordArray (select 'n' words)
						int res = wordArray.initializeWordArray( MainActivity.this, fileNameSelected );
						if( res == 1 ){
							Log.d( "upload", "ERROR: initializeWordArray( ) returned an error" );
							Toast.makeText(MainActivity.this, "Something went wrong. Could not start Game", Toast.LENGTH_SHORT).show();
							return; //error: could not initialize wordArray
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					Intent gameActivity = new Intent( MainActivity.this, GameActivity.class );
					state = 0;
					//check to see for language format is correct and available
					if (usrModePref == 1) {
						if (usrLangPref == 0) {
							language = wordArray.getTranslationLang();
							Log.e("lTTSs", "language is: "+language);
						}
						else {
							language = wordArray.getNativeLang();
							Log.e("lTTSs", "language is: "+language);
						}
						canStart = false;
						for (int i=0; i<lTTSlangTags.size(); i++) {
							//Log.e("lTTS", "language is: "+language+" langTag is: "+langTags.get(i));
							if (Objects.equals(language,lTTSlanguage.get(i))) {
								language = lTTSlangTags.get(i);
								canStart = true;
								if (canStart) {
									break;
								}
							}
						}
						if (canStart) {
							//save wordArray for Game Activity
							gameActivity.putExtra( "wordArray", wordArray );
							gameActivity.putExtra( "usrLangPref", usrLangPref );
							gameActivity.putExtra("usrDiffPref",usrDiffPref);
							gameActivity.putExtra("state", state);
							gameActivity.putExtra("usrModeMA", usrModePref);
							gameActivity.putExtra("languageMA", language);
							gameActivity.putExtra( "HINT_CLICK_TO_MAX_PROB", HINT_CLICK_TO_MAX_PROB );
							startActivityForResult(gameActivity,0);
						}
						else {
							Toast.makeText(v.getContext(),R.string.no_language, Toast.LENGTH_LONG).show();
						}
					}
					else {
						//standard start
						gameActivity.putExtra( "wordArray", wordArray );
						gameActivity.putExtra( "usrLangPref", usrLangPref );
						gameActivity.putExtra("usrDiffPref",usrDiffPref);
						gameActivity.putExtra("state", state);
						gameActivity.putExtra("usrModeMA", usrModePref);
						gameActivity.putExtra("languageMA", language);
						gameActivity.putExtra( "HINT_CLICK_TO_MAX_PROB", HINT_CLICK_TO_MAX_PROB );
						startActivityForResult(gameActivity,0);
					}
				}
			}
		);
		
		
		//implement STOP btn
		Button btnStop = (Button) findViewById( R.id.button_stop );
		btnStop.setOnClickListener( new View.OnClickListener(  )
									{
										@Override
										public void onClick( View v )
										{
											state = 0;
											btn_remove.setEnabled( true ); //allow user to remove pkg
											removeBtnEnable[0] = true;
											Button btnResume = findViewById( R.id.button_resume );
											btnResume.setEnabled( false );
										}
									}
		);
		
		
		Button btnResume = (Button) findViewById(R.id.button_resume);
		if (state == 0) {
			btnResume.setEnabled(false); //block Resume button unless a previous game is saved
			//DISABLE "REMOVE PKG" button when game is started
			removeBtnEnable[0] = true;
		}
		else {
			removeBtnEnable[0] = false;
		}
		
		
		//if resume button is unblocked and pressed, it will load previous game preferences
		btnResume.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (state==1) {
					//load previous game preferences to prepare for export to new Game Activity
					Intent resumeActivity = new Intent(MainActivity.this, GameActivity.class);
					//save preferences for Game Activity to read
					resumeActivity.putExtra("wordArrayMA", wordArrayResume);
					resumeActivity.putExtra("usrLangPrefMA", usrLangPrefResume);
					resumeActivity.putExtra("SudokuArrMA", usrSudokuArrResume);
					resumeActivity.putExtra("state", state);
					resumeActivity.putExtra("usrModeMA", usrModePrefResume);
					if (usrModePrefResume == 1) {
						resumeActivity.putExtra("numArrayMA", numArrayResume);
					}
					resumeActivity.putExtra("languageMA", languageResume);
					resumeActivity.putExtra( "HINT_CLICK_TO_MAX_PROB", HINT_CLICK_TO_MAX_PROB );
					startActivityForResult(resumeActivity, 0);
				}
				else {
					Toast.makeText(v.getContext(),R.string.no_resume, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	
	@Override
	public void onStart( )
	{
		super.onStart( );
		
			/** WHEN USER RETURNING FROM UPLOAD ACTIVITY, UPDATE WORD PKG LIST **/
			
		FileCSV fileCSV = new FileCSV( MAX_WORD_PKG, MAX_CSV_ROW, MIN_CSV_ROW );
		int CURRENT_WORD_PKG_COUNT_RETURN;
		try {
			CURRENT_WORD_PKG_COUNT_RETURN = fileCSV.findCurrentPackageCount( this ); //get current Packages count so far
		} catch (IOException e) {
			CURRENT_WORD_PKG_COUNT_RETURN = CURRENT_WORD_PKG_COUNT;
			e.printStackTrace();
		}
		
		// ENABLE OR DISABLE REMOVE BTN
		if( removeBtnEnable[0] == true ) //enable btn
		{
			Button btn = findViewById( R.id.btn_remove );
			btn.setEnabled( true );
		} else {
			Button btn = findViewById( R.id.btn_remove );
			btn.setEnabled( false );
		}
		
		
		// read all packages the user has uploaded so far, and get an array with name and file
		try {
			wordPackageFileIndexArr = new WordPackageFileIndex( this, MAX_WORD_PKG, CURRENT_WORD_PKG_COUNT_RETURN ); //allow a maximum of X packages
		} catch( IOException e ){
			e.printStackTrace( );
		}
		
		pkgRadioGroup = findViewById(R.id.pkg_radio_group);
		
		//update scroll pkg view
		updatePkgViewAfterUploadOrRemoval( CURRENT_WORD_PKG_COUNT_RETURN );
		
		Log.d( "upload", "onStart() called from MainActivity" );
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resumeSrc) {
		if (resumeSrc != null) {
			//if all necessary game preferences are written in memory, then unblock resume button
			if (resumeSrc.hasExtra("wordArrayGA") && resumeSrc.hasExtra("usrLangPrefGA") && resumeSrc.hasExtra("SudokuArrGA") && resumeSrc.hasExtra("usrModeGA") && resumeSrc.hasExtra("languageGA")) {
				Log.i("TAG", "resumeSrc has all elements");
				wordArrayResume = (WordArray) resumeSrc.getParcelableExtra("wordArrayGA");
				usrLangPrefResume = (int) resumeSrc.getSerializableExtra("usrLangPrefGA");
				usrSudokuArrResume = (SudokuGenerator) resumeSrc.getSerializableExtra("SudokuArrGA");
				usrModePrefResume = (int) resumeSrc.getSerializableExtra("usrModeGA");
				if (usrModePrefResume == 1) {
					numArrayResume = (String[]) resumeSrc.getStringArrayExtra("numArrayGA");
				}
				languageResume = (String) resumeSrc.getSerializableExtra("languageGA");
				state = 1;
				Button btnResume = (Button) findViewById(R.id.button_resume);
				btnResume.setEnabled(true);
				Button btnRemove = (Button) findViewById(R.id.btn_remove); //block user from deleting pkg while playing game
				btnRemove.setEnabled(false);
				removeBtnEnable[0] = false;
			}
		}
	}

	@Override
	public void onSaveInstanceState (Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("state", state);
		if (state == 1) {
			savedInstanceState.putParcelable("wordArrayMA", wordArrayResume);
			savedInstanceState.putInt("usrLangPrefMA", usrLangPrefResume);
			savedInstanceState.putSerializable("SudokuArrMA", usrSudokuArrResume);
			savedInstanceState.putInt("usrModeMA", usrModePrefResume);
			savedInstanceState.putString("languageMA", languageResume);
			savedInstanceState.putInt( "HINT_CLICK_TO_MAX_PROB", HINT_CLICK_TO_MAX_PROB );
			if (usrModePrefResume == 1) {
				savedInstanceState.putStringArray("numArrayMA", numArrayResume);
			}
		}
	}
	
	private int findUserPuzzleTypePreference( RadioGroup radGroup )
	{
		//find which puzzle type the user selected
		int usrPuzzleTypePref = -1;
		int btnID = radGroup.getCheckedRadioButtonId( );
		View radioBtn = radGroup.findViewById( btnID );
		usrPuzzleTypePref = radGroup.indexOfChild( radioBtn );
		return usrPuzzleTypePref - 1; //-1 because first index is TextView
	}
	
	
	private int checkIfJustInstalledAndSetUpPackagesAlreadyInstalled( )
	{
		//return 0 on success
		
			/* TEST IF USER JUST INSTALLED APP - IF USER HAS, LOAD DEFAULT FILES */
			/* ELSE, SET UP wordPackageFileIndexArr to store all packages so far, and create Package Scroll */
		
		int usrNewInstall = fileCSV.checkIfCurrentWordPkgCountFileExists( this ); //0==files already exist
		
		if( usrNewInstall == 0 ) //if app was already installed and has correct files - get current_word_pkg_count
			try {
				CURRENT_WORD_PKG_COUNT = fileCSV.findCurrentPackageCount( this ); //get current Packages count so far
			} catch (IOException e) {
				e.printStackTrace();
				return 1;
			}
		else //fresh app install
		{
			try
			{
				fileCSV.importDefaultPkg( this ); //load default Word Package
				CURRENT_WORD_PKG_COUNT = fileCSV.findCurrentPackageCount( this ); //get current Packages count so far
			} catch( IOException e ) {
				e.printStackTrace( );
				return 1;
			}
		}
		
		// read all packages the user has uploaded so far, and get an array with name and file
		try {
			wordPackageFileIndexArr = new WordPackageFileIndex( this, MAX_WORD_PKG, CURRENT_WORD_PKG_COUNT ); //allow a maximum of X packages
		} catch( IOException e ){
			e.printStackTrace( );
			return 1;
		}
		
			/* UPDATE THE WORD PKG SCROLL */
		
		RadioButton radBtn;
		
		for( int i=0; i<CURRENT_WORD_PKG_COUNT; i++ )
		{
			radBtn = new RadioButton( this );
			
			radBtn.setText( wordPackageFileIndexArr.getPackageFileAtIndex( i ).getWordPackageName( ) );
			
			pkgRadioGroup.addView(radBtn);
		}
		
		//automatically select first button
		( (RadioButton) (pkgRadioGroup.getChildAt(0)) ).setChecked( true );
		
		return 0;
	}
	
	
	private void updatePkgViewAfterUploadOrRemoval( int CURRENT_WORD_PKG_COUNT_RETURN )
	{
		/*
		 * After user returns from Uploading or Removing Activity
		 * This function updates the user Scroll View with what packages are available after removal/upload
		 */
		RadioButton radBtn;
		
		if( CURRENT_WORD_PKG_COUNT_RETURN > CURRENT_WORD_PKG_COUNT ) //usr uploaded a pkg
		{
			/* USER UPLOADED A PKG */
			
			CURRENT_WORD_PKG_COUNT = CURRENT_WORD_PKG_COUNT_RETURN; //update pkg count because it increased
			Log.d("upload", "USER UPLOADED A PKG");
			//find how many pkg are available and if user user
			for( int i=pkgRadioGroup.getChildCount(); i<CURRENT_WORD_PKG_COUNT; i++ )
			{
				radBtn = new RadioButton(this);
				
				//radBtn.setText( allPkgName[i] );
				radBtn.setText(wordPackageFileIndexArr.getPackageFileAtIndex(i).getWordPackageName());
				
				pkgRadioGroup.addView(radBtn);
				
			}
		}
		else if( CURRENT_WORD_PKG_COUNT_RETURN < CURRENT_WORD_PKG_COUNT )
		{
			/* USER DELETED A PKG */
			
			Log.d("upload", "USER DELETED A PKG");
			CURRENT_WORD_PKG_COUNT = CURRENT_WORD_PKG_COUNT_RETURN; //update pkg count because it decreased
			
			
			// IMPORTANT: the following procedure limits to deleting only 1 file at a time
			//			  to delete multiple files at one, have to delete and re-create all radio buttons in RadioGroup
			
			pkgRadioGroup.removeViewAt( indexOfRadBtnToRemove[0] );
			
			//reselect top radio btn
			( (RadioButton) pkgRadioGroup.getChildAt(0)).setChecked( true );
			
			
		}
		else
		{
			Log.d("upload", "USER DID NOT MODIFY A PKG");
		}
	}
}









