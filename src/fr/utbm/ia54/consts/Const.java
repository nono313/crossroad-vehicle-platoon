package fr.utbm.ia54.consts;


import java.util.logging.Level;

/**
 * Constants class.
 * @author Alexis Florian
 */
public class Const {
	
	/* Car settings */
	public static final float 	ACC = 200;
	public static final float 	DECC = -300;
	public static final int 	PAS=35;
	
	public static float debugAccelerator = 1.f;
	
	
	/* Simulation settings */
	public static final int NB_CAR_BY_TRAIN = 5;
	public static int NB_TRAIN;
	
	/* Community, role and group constants */
	public static final String MY_COMMUNITY="ia54"; //$NON-NLS-1$
    public static final String SIMU_GROUP="train"; //$NON-NLS-1$
    public static final String CAR_ROLE = "car"; //$NON-NLS-1$
    public static final String TRAIN_ROLE = "train"; //$NON-NLS-1$
    public static final String LAST_CAR_ROLE = "last"; //$NON-NLS-1$
    public static final String ENV_ROLE = "environment"; //$NON-NLS-1$
    public static final String SCH_ROLE = "scheduler"; //$NON-NLS-1$
    public static final String VIEWER_ROLE = "viewer"; //$NON-NLS-1$

    
    /* Application settings */
    public static final String RESOURCES_DIR = "ressources"; //$NON-NLS-1$
    public static final int CAR_SIZE= 32;
    public static final String CAR_COLOR[] = {"voiture_verte.png", "voiture_blanche.png", "voiture_rouge.png"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
    /* Debug levels */
    public static final Level LOGGER_MESSAGES_SENT = Level.FINE;
}
