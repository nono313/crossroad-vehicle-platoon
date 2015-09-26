package fr.utbm.ia54.consts;

/**
 * Constants class.
 * @author Alexis Florian
 */
public class Const {
	
	/* Car settings */
	public static final float 	ACC = 100;
	public static final float 	DECC = 300;
	public static final int 	PAS=35;
	
	
	/* Simulation settings */
	public static final int NB_CAR_BY_TRAIN = 5;
	public static int NB_TRAIN;
	
	/* Community, role and group constants */
	public static final String MY_COMMUNITY="ia54";
    public static final String SIMU_GROUP="train";
    public static final String CAR_ROLE = "car";
    public static final String TRAIN_ROLE = "train";
    public static final String LAST_CAR_ROLE = "last";
    public static final String ENV_ROLE = "environment";
    public static final String SCH_ROLE = "scheduler";
    public static final String VIEWER_ROLE = "viewer";

    
    /* Application settings */
    public static final String RESOURCES_DIR = "ressources";
    public static final int CAR_SIZE= 32;
    public static final String CAR_COLOR[] = {"voiture_verte.png", "voiture_blanche.png", "voiture_rouge.png"};
}
