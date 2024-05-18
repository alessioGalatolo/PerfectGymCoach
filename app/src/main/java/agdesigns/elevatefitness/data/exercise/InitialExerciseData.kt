package agdesigns.elevatefitness.data.exercise

import agdesigns.elevatefitness.R

// TODO: Fix general spelling and capitalisation, missing exercises e.g. planche
val INITIAL_EXERCISE_DATA = listOf(
    /*
        CHEST EXERCISES
     */
    // Barbell chest
    Exercise(
        name = "Bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie on a bench with the barbell racked above your chest. Grasp the bar with an overhand grip, slightly wider than shoulder-width apart. Unrack the bar and lower it slowly to your chest, keeping your elbows tucked in. Press the bar back up to the starting position, extending your arms fully."
    ),
    Exercise(
        name = "Inclined bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.incline_bench_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie on an inclined bench (head above hips) with the barbell racked above your chest. Grasp the bar with an overhand grip, slightly wider than shoulder-width apart. Unrack the bar and lower it slowly to your chest, keeping your elbows tucked in. Press the bar back up to the starting position, extending your arms fully."
    ),
    Exercise(
        name = "Declined bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.bench_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie on a declined bench (hips above head) with the barbell racked above your chest. Grasp the bar with an overhand grip, slightly wider than shoulder-width apart. Unrack the bar and lower it slowly to your chest, keeping your elbows tucked in. Press the bar back up to the starting position, extending your arms fully."
    ),

    // Cables chest
    Exercise(
        name = "Cable crossover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.CHEST,
        image = R.drawable.cable_crossover,
        description = "To perform a cable crossover, start by setting the handles on a dual cable machine just above your head height. Stand in the middle of the machine with your feet shoulder-width apart, slightly bent knees, and a slight forward lean. Grasp the handles with your palms facing down. With a smooth motion, draw your hands towards each other in a wide arc until they meet in front of your chest. Make sure to keep your elbows slightly bent throughout the motion. Slowly return the handles along the same path back to the starting position, controlling the weight as you go. Focus on squeezing your chest muscles as you bring the handles together."
    ),

    // Bodyweight chest
    Exercise(
        name = "Chest dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.chest_dip,
        description = "To perform a chest dip, approach the parallel bars and grasp them with a firm grip. Jump up and straighten your arms to lift your body off the ground, stabilizing yourself above the bars. Lean your torso slightly forward to emphasize the chest muscles. Bend your elbows to lower your body, aiming to descend until your shoulders are slightly below your elbows or until you feel a full stretch in your chest. Push through your palms to lift yourself back to the starting position, focusing on using your chest muscles to do the work. Avoid locking your elbows at the top to maintain tension on the muscles."
    ),
    Exercise(
        name = "Push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.push_up,
        description = "To do a push-up, start in a plank position with your hands placed slightly wider than shoulder-width apart directly under your shoulders. Your body should form a straight line from your head to your heels, which are held together. Engage your core and keep your back flat. Lower your body towards the floor by bending your elbows out to the sides until your chest is just above the ground. Ensure your elbows form about a 45-degree angle with your body at the bottom of the movement. Press through your hands, extending your elbows to raise your body back to the starting position. Keep your movements smooth and controlled throughout the exercise.",
        variations = listOf(
            "Wide arms",
            "Inclined",
            "Declined",
            "Single-arm"
        )
    ),

    // Machine chest
    Exercise(
        name = "Chest press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS), // Removed otherwise is considered compound
        image = R.drawable.chest_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To perform a machine chest press, start by adjusting the seat of the machine so that the handles are at chest level when you're seated. Sit down and grip the handles with your palms facing down or facing each other, depending on the machine design. Plant your feet firmly on the ground and keep your back pressed against the seat pad.\n" +
                "Push the handles away from your chest until your arms are extended but not locked at the elbows. Focus on contracting your chest muscles as you press. Pause briefly at the full extension, then slowly bring the handles back towards your chest, controlling the weight as you move. Ensure your movements are smooth and steady, and maintain a steady breathing pattern by exhaling as you press and inhaling as you return to the starting position.",
    ),
    Exercise(
        name = "Machine fly",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = emptyList(),
        image = R.drawable.machine_fly,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To perform a machine fly, start by adjusting the seat so that when you sit, the machine's handles align with your chest. Sit down and grip the handles with your palms facing each other. Ensure your back is firmly against the backrest and plant your feet flat on the floor.\n"+
                "With a slight bend in your elbows (maintain this angle throughout the exercise), bring your arms together in a smooth, arcing motion in front of your chest. Squeeze your chest muscles as the handles meet in front of you. Hold the contraction briefly at the peak of the movement." +
                "Slowly reverse the motion, allowing the handles to move back to the starting position while controlling the resistance. Keep your movements fluid and controlled, focusing on the stretch and contraction of the chest muscles throughout the exercise. Remember to breathe out as you bring the handles together and breathe in as you return to the starting position."
    ),

    // Dumbbell chest
    Exercise(
        name = "Dumbbell bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.dumbbell_bench_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To perform a dumbbell bench press, begin by lying flat on a bench with a dumbbell in each hand, held at chest level. Your feet should be flat on the floor for stability. Press the dumbbells up and away from your chest until your arms are fully extended above you, but don’t lock your elbows. Keep a slight bend in them at the top of the movement. Lower the dumbbells slowly back to the starting position at chest level, ensuring you maintain control throughout the movement. Focus on keeping your chest muscles engaged and avoid letting the dumbbells drift too far towards your head or abdomen to maintain proper form.",
        variations = listOf(
            "Inclined",
            "Declined",
            "Single-arm"
        )
    ),
    Exercise(
        name = "Dumbbell fly",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),  // FIXME: It should be an isolation exercise, maybe no secondary muscles
        image = R.drawable.dumbbell_bench_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "\n" +
                "To perform a dumbbell fly, lie on a flat bench holding a dumbbell in each hand directly above your chest with your palms facing each other. Start with your arms slightly bent at the elbows, a bend you should maintain throughout the exercise to protect your joints. Slowly lower the dumbbells in an arc out to the sides of your body, keeping the motion controlled, until you feel a stretch in your chest. Avoid lowering the weights too far, as this can strain your shoulders. Bring the dumbbells back up along the same wide arc, squeezing your chest muscles together at the top of the movement. Focus on using your chest to pull the weights and keep the movement smooth without jerking the dumbbells.",
        variations = listOf(
            "Inclined",
            "Declined"
        )
    ),
    Exercise(
        name = "Pullover",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CHEST,
        secondaryMuscles = listOf(Exercise.Muscle.BACK),
        image = R.drawable.generic_dumbbell,
        description = "To perform a pullover, start by lying on a bench with your head near the edge and a dumbbell held with both hands above your chest, arms extended. The dumbbell should be held in a vertical position with your hands supporting one of the weights from underneath. Keep your hips slightly lower than the bench to engage your core. Lower the dumbbell in an arc behind your head while keeping your arms straight, stretching your chest and lats until your arms are in line with the bench. Bring the dumbbell back over your chest by reversing the arc, maintaining the straight arm position throughout. This exercise engages both your chest and your upper back."
    ),

    /*
        BACK
     */
    // Barbell back
    Exercise(
        name = "Deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        image = R.drawable.deadlift,
        description = "To perform a deadlift, stand with your feet hip-width apart, toes pointing forward, and the barbell over the center of your feet. Bend at your hips and knees to reach down and grasp the bar with a grip slightly wider than shoulder width, alternating one hand facing forwards and the other facing back for stability. Keep your back flat and your chest up as you lift the bar by extending your hips and knees. The bar should stay close to your legs, almost touching. Drive through your heels, not the balls of your feet. As the bar passes your knees, push your hips forward to stand up straight. Lower the bar by bending at the hips and controlling the descent without rounding your back."
    ),
    Exercise(
        name = "Sumo deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        image = R.drawable.sumo_deadlift,
        description = "To perform a sumo deadlift, position your feet wider than shoulder-width apart with your toes pointing outwards, much like a sumo wrestler’s stance. The bar should be close to your shins. Bend at the hips to reach down and grip the bar between your legs using an overhand grip or a mixed grip (one hand over, one hand under). Keep your back straight and chest up to ensure a strong, neutral spine position. Drive through your heels, pulling the bar up while straightening your hips and knees simultaneously. The bar should travel straight up, close to your body. Once the bar passes your knees, thrust your hips forward to lock out at the top. Lower the bar by bending at the hips first, then the knees, while maintaining a flat back throughout the movement."
    ),
    Exercise(
        name = "Barbell shrug",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_barbell,
        description = "To perform a barbell shrug, stand with your feet about shoulder-width apart and hold a barbell in front of you with an overhand grip, hands slightly wider than shoulder-width. Let the bar hang at arm's length in front of your thighs. Keep your back straight and your shoulders relaxed. Lift your shoulders straight up towards your ears in a shrugging motion without bending your elbows or using your biceps. Hold the contraction at the top for a moment, then slowly lower your shoulders back to the starting position. Focus on the upward and downward movement being controlled and on using your trapezius muscles to perform the shrug. Avoid rolling your shoulders as this can cause strain."
    ),
    Exercise(
        name = "Barbell row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.ABS),
        image = R.drawable.barbell_row,
        description = "To perform a barbell row, start by standing with your feet hip-width apart and a barbell placed in front of you. Bend your knees slightly and hinge at the hips to lean forward, keeping your back straight and nearly parallel to the floor. Grip the barbell with both hands using an overhand grip, slightly wider than shoulder-width. Pull the bar towards your lower ribs while keeping your elbows close to your body and squeezing your shoulder blades together at the top of the movement. Lower the bar back to the starting position in a controlled manner, ensuring it remains close to your body throughout the exercise. Keep your head neutral and core engaged to support your back during the movement."
    ),
    Exercise(
        name = "Barbell t-bar row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        image = R.drawable.generic_barbell,
        description = "To perform a barbell T-bar row, start by standing over the bar with feet shoulder-width apart. Grip the handle attached to one end of the barbell, and while keeping your knees slightly bent, lean forward from your hips until your torso is almost parallel to the floor. Maintain a straight back, pull the barbell towards your chest just below your ribs, engaging the middle back muscles. Lower the barbell back to the starting position in a controlled manner to complete one rep."
    ),
    Exercise(
        name = "Barbell upright row",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        image = R.drawable.generic_barbell,
        description = "To perform a barbell upright row, stand with your feet hip-width apart and grasp a barbell with an overhand grip that is slightly narrower than shoulder width. With your arms extended and the barbell resting against your thighs, lift the barbell straight up to your chin, leading with your elbows and keeping the bar close to your body. Pause briefly when your elbows are at shoulder height, then lower the barbell back to the starting position in a controlled motion to complete one rep."
    ),

    // Cables back
    Exercise(
        name = "Cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.cable_row,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To perform a cable row, begin by sitting at a cable row machine with your feet firmly planted on the platform and your knees slightly bent. Grab the handle with both hands using an overhand grip. Sit back with your arms extended, keeping your back straight and slightly leaning back from the hips. Pull the handle towards your lower abdomen, squeezing your shoulder blades together while keeping your elbows close to your body. Slowly extend your arms back to the starting position, stretching your shoulder blades apart to complete one rep."
    ),
    Exercise(
        name = "Cable pullover",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_cable,
        description = "To perform a cable pullover, start by attaching a straight bar to a high pulley of a cable machine. Stand facing the machine with your feet shoulder-width apart and grasp the bar with an overhand grip, hands wider than shoulder width. With a slight bend in your knees and your torso hinged slightly forward from the hips, pull the bar down in an arc until it is in line with your thighs, keeping your arms extended and elbows slightly bent throughout the movement. Slowly return the bar to the starting position, maintaining control and keeping tension in your lats and triceps to complete one rep."
    ),
    Exercise(
        name = "Upright cable row",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.generic_cable,
        description = "To perform an upright cable row, stand in front of a low pulley cable machine and attach a straight bar to the pulley. Grab the bar with both hands using an overhand grip, hands slightly narrower than shoulder width apart. Stand up straight with your arms extended, allowing the weight to help stretch your shoulders downward. Pull the bar straight up towards your chin, keeping it close to your body, and lifting your elbows high and out to the sides. Pause briefly at the top when the bar is near your chin, then slowly lower the bar back to the starting position to complete one rep."
    ),

    // Bodyweight back
    Exercise(
        name = "Pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.SHOULDERS), // Shoulders have been added to make the exercise compound
        image = R.drawable.pull_up,
        description = "To perform a pull-up, start by gripping a pull-up bar with an overhand grip, hands placed slightly wider than shoulder-width apart. Hang from the bar with your arms fully extended and legs off the ground, either straight down or crossed at the ankles. Pull your body up by driving your elbows down to the floor until your chin is above the bar, focusing on squeezing your shoulder blades together. Slowly lower yourself back to the starting position, keeping control as you fully extend your arms to complete one rep.",
        variations = listOf(
            "Single arm"
        )
    ),
    Exercise(
        name = "Wide grip pull up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.wide_pull_up,
        description = "To perform a pull-up, start by gripping a pull-up bar with an overhand grip, hands placed much wider than shoulder-width apart. Hang from the bar with your arms fully extended and legs off the ground, either straight down or crossed at the ankles. Pull your body up by driving your elbows down to the floor until your chin is above the bar, focusing on squeezing your shoulder blades together. Slowly lower yourself back to the starting position, keeping control as you fully extend your arms to complete one rep.",
        ),
    Exercise(
        name = "Chin up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.chin_up,
        description = "To perform a chin-up, grip a pull-up bar with your palms facing towards you and hands shoulder-width apart. Hang fully extended from the bar, then pull yourself up until your chin is above the bar. Slowly lower yourself back to the starting position and repeat.",
        variations = listOf(
            "Close grip"
        )
    ),
    Exercise(
        name = "Muscle up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS, Exercise.Muscle.CHEST, Exercise.Muscle.TRICEPS, Exercise.Muscle.SHOULDERS),
        image = R.drawable.muscle_up,
        difficulty = Exercise.ExerciseDifficulty.ADVANCED,
        description = "Start by hanging from the bar with an overhand grip. Pull yourself up explosively while simultaneously pushing down on the bar and pulling your hips up towards the bar. Once your chest reaches bar level, transition by pushing the bar away from you and pulling your body above it. Finish by straightening your arms. Reverse the movement to return to the starting position.",
    ),
    Exercise(
        name = "Rope climbing",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.rope_climb,
        description = "To do rope climbing, start by grasping the rope with both hands above your head and your feet off the ground. Use your arms to pull your body up while simultaneously wrapping your legs around the rope for support. Alternate between pulling with your arms and stepping higher with your legs to ascend. To descend, carefully reverse the process, controlling your descent with your arms and legs.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),

    // Machine back
    Exercise(
        name = "Machine row",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "For the machine row, first adjust the seat and chest pad for your height. Sit down and plant your feet firmly on the platform. Grasp the handles with both hands, and pull them towards your torso while keeping your back straight and elbows close to your body. Squeeze your shoulder blades together at the end of the movement, then slowly return the handles to the starting position and repeat.",
        image = R.drawable.cable_row
    ),
    Exercise(
        name = "Hyperextensions",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To perform hyperextensions, position yourself on a hyperextension bench with your ankles secured under the footpads. Lean forward, bending at the waist, and ensure your body is straight. Cross your arms over your chest or place your hands behind your head. Slowly raise your upper body until it is in line with your legs, then lower back down to the starting position without rounding your back. Repeat the movement.",
        image = R.drawable.hyperextensions
    ),
    Exercise(
        name = "Lat pulldown", // also called lat machine
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        image = R.drawable.lat_pulldown,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "For the lat pulldown, sit at the machine and grab the bar with an overhand grip, hands wider than shoulder width. Adjust the knee pad to fit snugly against your legs to prevent lifting. Lean back slightly, engage your core, and pull the bar down to your chest, drawing your shoulder blades down and back. Slowly extend your arms to return the bar to the starting position, maintaining control throughout the movement. Repeat as necessary.",
        variations = listOf(
            "V-bar"
        )
    ),
    Exercise(
        name = "Vertical traction",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        description = "To perform vertical traction, sit at the vertical traction machine and adjust the thigh pads to secure your legs. Grasp the handles above with a wide overhand grip. Pull the handles down smoothly towards your shoulders, keeping your elbows pointed straight down and slightly back. Engage your back muscles as you pull. Slowly release the handles back to the starting position, maintaining control and keeping tension in your back muscles throughout. Repeat the exercise as required.",
        image = R.drawable.lat_pulldown
    ),

    // Dumbbell back
    Exercise(
        name = "Dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.BICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "To do a dumbbell row, place one knee and the same-side hand on a bench for support, ensuring your back is flat and parallel to the ground. Hold a dumbbell in your free hand with an overhand grip, arm extended. Pull the dumbbell upwards to the side of your chest, keeping your arm close to your side and your torso stationary. Focus on pulling with your back muscles, not just your arm. Lower the dumbbell slowly back to the starting position and repeat for the desired number of repetitions before switching sides.",
        image = R.drawable.dumbbell_row
    ),
    Exercise(
        name = "Dumbbell deadlift",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        description = "To perform a dumbbell deadlift, start by standing with your feet hip-width apart, holding a dumbbell in each hand in front of your thighs. Your palms should be facing your body. Push your hips back and slightly bend your knees, lowering the dumbbells along the front of your legs until they reach about mid-shin level. Keep your back flat and look forward to keep your neck in a neutral position. Drive through your heels to return to a standing position, pushing your hips forward and bringing the dumbbells back up to the starting position. Repeat the movement for the desired number of repetitions.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell shrug",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = emptyList(),
        image = R.drawable.dumbbell_shrug,
        description = "To perform a dumbbell shrug, stand upright with your feet shoulder-width apart and a dumbbell in each hand, arms by your sides, palms facing inwards. Keep your arms straight and lift your shoulders towards your ears in a shrugging motion. Hold the contraction at the top for a moment, then slowly lower your shoulders back to the starting position. Repeat the exercise for the desired number of repetitions, maintaining a controlled movement throughout.",
    ),
    Exercise(
        name = "Upright dumbbell row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BACK,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        description = "Stand with feet shoulder-width apart, holding dumbbells in front with palms facing body. Lift dumbbells vertically to chest, keeping them close to your body, and elbows pointed outwards. Lower back slowly.",
        image = R.drawable.generic_dumbbell
    ),

    /*
        ABS
     */
    // Barbell abs

    // Cables abs
    Exercise(
        name = "Cable crunch",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Kneel below a cable machine with a rope attachment. Hold the rope with both hands and position your hands near your face. Crunch down towards your knees using your abs, pulling the rope and your head towards the ground. Return to start slowly.",
        image = R.drawable.generic_cable
    ),

    // Bodyweight abs
    Exercise(
        name = "Crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie on your back with knees bent and feet flat on the ground. Place hands behind your head. Lift your upper body towards your knees using your core muscles. Slowly lower back down.",
        image = R.drawable.crunch
    ),
    Exercise(
        name = "Knee raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Hang from a pull-up bar with hands shoulder-width apart. Raise knees towards your chest while keeping the lower back straight. Lower them back down slowly.",
        image = R.drawable.knee_raises
    ),
    Exercise(
        name = "Leg raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie on your back with legs straight and hands under your hips for support. Lift legs straight up to the ceiling, then lower them back down without touching the floor.",
        image = R.drawable.leg_raises
    ),
    Exercise(
        name = "Plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie face down, then lift your body off the ground with your forearms and toes holding you up. Keep your body straight and hold the position.",
        image = R.drawable.plank
    ),
    Exercise(
        name = "Russian twist",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Sit on the floor with knees bent and feet off the ground. Lean back slightly. With a weight in your hands, twist your torso from left to right, engaging your core.",
        image = R.drawable.russian_twist
    ),
    Exercise(
        name = "Side plank",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Lie on your side with legs extended. Prop your upper body up on your forearm, keeping your body in a straight line. Hold, then switch sides.",
        image = R.drawable.side_plank
    ),
    Exercise(
        name = "Sit ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Lie on your back with knees bent and feet anchored. Cross your arms over your chest. Sit all the way up towards your knees, then slowly lower back down.",
        image = R.drawable.sit_ups
    ),
    Exercise(
        name = "Side crunch",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Lie on your side with legs stacked and knees slightly bent. Place one hand behind your head. Crunch your upper body towards your hips. Repeat on the other side.",
        image = R.drawable.side_crunch
    ),
    Exercise(
        name = "Dragon fly",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Lie on a bench and secure your upper body with straps or by holding the bench above your head. Extend your legs straight out, then lift them towards the ceiling while keeping them straight, then lower back to start without touching the bench.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    Exercise(
        name = "Ab roller",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Kneel with an ab roller in front of you. Roll the ab roller forward, extending your body into a straight line, then roll it back towards your knees while keeping your core tight.",
        image = R.drawable.ab_roller
    ),

    // Machine abs
    Exercise(
        name = "Ab machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Sit on the ab machine with your back against the pad. Place your feet under the foot pads and grasp the handles. Curl your upper body towards your knees, squeezing your abs, then slowly return to the starting position.",
        image = R.drawable.generic_machine
    ),

    // Dumbbell abs
    Exercise(
        name = "Dumbbell side bend",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.ABS,
        secondaryMuscles = emptyList(),
        description = "Stand upright holding a dumbbell in one hand, with the other hand placed on your waist. Lean to the side with the dumbbell, keeping your back straight and abs tight, then return to the upright position. Repeat on the other side.",
        image = R.drawable.generic_dumbbell
    ),

    /*
        BICEPS
     */
    // Barbell biceps
    Exercise(
        name = "Barbell curl",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.barbell_curl,
        description = "Stand up straight with a barbell held at hip level, palms facing forward. Curl the barbell up towards your chest while keeping your elbows close to your body. Lower the barbell back down with control.",
        variations = listOf(
            "Preacher",
            "Scott"
        )
    ),

    // Cables biceps
    Exercise(
        name = "Cable curl (with bar)",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.cable_curl,
        description = "Attach a straight bar to a low pulley cable machine. Stand facing the machine, grab the bar with both hands, palms facing up. Curl the bar towards your chest, keeping your elbows stationary, then slowly lower the bar back down.",
        variations = listOf(
            "Bar",
            "Rope",
            "Handles"
        )
    ),

    // Bodyweight

    // Machine
    Exercise(
        name = "Machine biceps curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.generic_machine,
        description = "Sit on the machine with your upper arms against the pads. Grasp the handles with palms facing up. Curl the handles towards your shoulders and slowly release back to the starting position.",
        variations = listOf(
            "Scott",
            "Preacher"
        )
    ),

    // Dumbbell
    Exercise(
        name = "Dumbbell curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.dumbbell_curl,
        description = "Stand with feet shoulder-width apart, holding a dumbbell in each hand with palms facing forward. Curl the weights while keeping your elbows close to your torso. Lower them back down slowly.",
        variations = listOf(
            "Alternating",
            "Inclined bench"
        )
    ),
    Exercise(
        name = "Dumbbell concentration curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        description = "Sit on a bench with your legs spread. Hold a dumbbell with one hand and rest your elbow on your thigh, just inside your knee. Curl the dumbbell towards your chest, keep your upper arm stable. Slowly lower the weight back down.",
        image = R.drawable.concentration_curl
    ),
    Exercise(
        name = "Dumbbell scott curl",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.BICEPS,
        secondaryMuscles = emptyList(),
        image = R.drawable.scott_dumbbell,
        description = "Sit at a preacher bench with your armpits at the top of the pad, holding a dumbbell in each hand. Curl the dumbbells while keeping your back of the arms against the pad. Lower the weights slowly back to the starting position.",
        variations = listOf(
            "Preacher"
        )
    ),

    /*
        TRICEPS
     */
    // Barbell triceps
    Exercise(
        name = "Barbell triceps extensions",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Lie on a bench with a barbell held above your chest, arms extended. Slowly lower the barbell behind your head by bending your elbows, keeping your upper arms stationary. Extend your arms back to the starting position.",
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Close grip bench press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        description = "Lie on a bench with a barbell held with a narrow grip, hands just inside shoulder width. Lower the barbell to your mid-chest, keeping your elbows close to your body. Press the barbell back up to the starting position.",
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "French press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Sit on a bench with a back support, holding a barbell or dumbbell with both hands overhead. Bend your elbows to lower the weight behind your head, then extend your arms back to the start.",
        image = R.drawable.generic_barbell
    ),
    Exercise(
        name = "Skull crusher",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Lie on a bench with a barbell or dumbbells. Extend arms straight up, then bend the elbows to lower the weights towards your forehead. Extend your arms back to the starting position.",
        image = R.drawable.generic_barbell
    ),

    // Cables
    Exercise(
        name = "Cable skull crusher",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Attach a rope to a low pulley cable machine. Lie on your back perpendicular to the machine, grabbing the rope with both hands. Extend your arms straight up, then bend the elbows to lower the rope towards your forehead. Extend back to start.",
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable pushdown",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand in front of a cable machine with a rope or bar attachment. Hold the attachment with an overhand grip and arms extended. Bend your elbows to push the attachment down until your arms are fully extended, then slowly return to start.",
        image = R.drawable.cable_pushdown
    ),
    Exercise(
        name = "Overhead cable triceps extension",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Attach a rope to a cable machine set above head height. Facing away from the machine, grab the rope with both hands and extend your arms with your hands behind your head. Extend your elbows to raise your hands above your head, then return to start.",
        image = R.drawable.generic_cable
    ),

    // Bodyweight
    Exercise(
        name = "Triceps dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        description = "Use parallel bars or a dip station. Grip the bars and hoist yourself up, arms straight. Bend your elbows to lower your body, keeping elbows close to your body. Push back up to the starting position.",
        image = R.drawable.chest_dip
    ),
    Exercise(
        name = "Bench dip",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.SHOULDERS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Place your hands on a bench behind you with your legs extended forward. Lower your body by bending your elbows until they are at about 90 degrees, then push back up to the starting position.",
        image = R.drawable.bench_dip
    ),
    Exercise(
        name = "Diamond push up",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        description = "Get into a push-up position but with your hands together under your chest, thumbs and index fingers touching to form a diamond shape. Lower your body towards the ground, then push back up.",
        image = R.drawable.push_up
    ),
    Exercise(
        name = "Parallel arms push up", // TODO: right name?
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        description = "Place your hands parallel and shoulder-width apart on the ground. Lower your body in a straight line by bending your elbows, then push back up to the starting position.",
        image = R.drawable.push_up
    ),

    // Machine
    Exercise(
        name = "Machine Triceps extension",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit on the machine with your back against the pad, arms bent and holding the handles. Extend your arms to push the handles down until fully straightened, then return slowly to start.",
        image = R.drawable.generic_machine
    ),

    // Dumbbell
    Exercise(
        name = "Dumbbell triceps extensions",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Stand or sit with a dumbbell held by both hands overhead. Bend your elbows to lower the dumbbell behind your head, then straighten your arms to lift it back up.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Close grip dumbbell bench press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.CHEST, Exercise.Muscle.SHOULDERS),
        description = "Lie on a bench holding two dumbbells with hands close together, directly above your chest. Lower the dumbbells to your chest, keeping your elbows close to your body, then press them back up.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell skull crusher",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.TRICEPS,
        secondaryMuscles = emptyList(),
        description = "Lie on a bench holding two dumbbells, arms extended straight up. Bend your elbows to lower the dumbbells towards your temples, then extend your arms back to the starting position.",
        image = R.drawable.generic_dumbbell
    ),

    /*
        QUADS, GLUTES, HAMSTRINGS (was LEGS) TODO: split by muscle
     */
    // Barbell
    Exercise(
        name = "Barbell hip thrust",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.ABS),
        image = R.drawable.generic_barbell,
        description = "Sit on the ground with your upper back against a bench, feet flat on the floor. Roll a barbell over your hips. Drive through your heels to lift your hips, squeezing your glutes at the top, then lower back down.",
    ),
    Exercise(
        name = "Barbell clean",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.ABS, Exercise.Muscle.BACK),
        image = R.drawable.barbell_clean,
        description = "Begin with a barbell on the floor close to your shins. Bend at your hips and knees to grip the barbell with an overhand grip. Lift the bar by extending your knees and hips. As the bar rises close to your thighs, pull yourself under the bar, catching it at your shoulders while squatting, then stand up.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    Exercise(
        name = "Squat",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        image = R.drawable.barbell_squat,
        description = "Stand with feet shoulder-width apart, toes slightly pointed out. Bend your hips and knees to lower your body as if sitting back into a chair, keeping your chest up and back straight, then return to standing.",
        variations = listOf(
            "Front",
            "Hack"
        )
    ),
    Exercise(
        name = "Barbell lunge",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.ABS, Exercise.Muscle.CALVES),
        description = "Stand with a barbell on your upper back. Step forward with one leg, lowering your hips to drop your back knee toward the floor. Keep your front knee over the ankle and back straight. Push back to the starting position and repeat on the other side.",
        image = R.drawable.barbell_lunge
    ),
    Exercise(
        name = "Romanian deadlift",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        image = R.drawable.romanian_deadlift,
        description = "Hold a barbell at hip level with a grip slightly wider than shoulder width. With knees slightly bent, push your hips back and lower the barbell along your thighs, keeping it close to your body until you feel a stretch in your hamstrings, then return to the start.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),

    // Cables
    Exercise(
        name = "Cable leg curl",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = emptyList(),
        description = "Attach an ankle cuff to a low cable pulley. Face the machine, attach the cuff to your ankle, and curl your leg towards your buttocks against the resistance, then slowly return to the starting position.",
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable pull-through",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS, Exercise.Muscle.BACK),
        description = "Stand facing away from a low cable pulley with a rope attachment between your legs. Hinge at your hips to pull the rope through, squeezing your glutes at the top, then return to the starting position.",
        image = R.drawable.cable_row
    ),

    // Bodyweight
    Exercise(
        name = "Bodyweight squat",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        image = R.drawable.squat,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand with feet shoulder-width apart. Bend your knees and hips to lower your body as though sitting in a chair, then push back up to standing.",
        variations = listOf(
            "Single leg"
        )
    ),
    Exercise(
        name = "Bodyweight step ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand in front of a bench or step. Step up with one foot, press through your heel to lift your body onto the step, then step down and repeat with the other leg.",
        image = R.drawable.step_ups
    ),
    Exercise(
        name = "Bodyweight lunge",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        description = "Stand with feet hip-width apart. Step forward with one leg and lower your hips to drop your back knee toward the ground, keeping the front knee over the ankle. Push back to the starting position and switch legs.",
        image = R.drawable.lunge
    ),

    // Machine
    Exercise(
        name = "Machine leg curl",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Lie face down on the leg curl machine with your legs under the pad. Flex your knees to pull the pad towards your buttocks, then slowly return to the starting position.",
        image = R.drawable.leg_machine
    ),
    Exercise(
        name = "Machine leg extension",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit on the machine with your legs under the pad and feet pointed forward. Extend your legs to lift the weight, then lower back to the starting position.",
        image = R.drawable.leg_machine
    ),
    Exercise(
        name = "Machine hack squat",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        description = "Position yourself in a hack squat machine with your back against the pad and shoulders under the shoulder pads. Bend your knees and lower down into a squat, then press back up to the starting position.",
        image = R.drawable.generic_machine
    ),
    Exercise(
        name = "Leg press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit in a leg press machine with your back against the pad and feet on the platform. Bend your knees to lower the platform toward you, then press the platform away until your legs are extended.",
        image = R.drawable.leg_press
    ),
    Exercise(
        name = "Abduction machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        description = "Sit in the machine with your back against the pad and legs inside the leg pads. Push legs outwards against the resistance, then slowly bring them back together.",
        image = R.drawable.generic_machine
    ),
    Exercise(
        name = "Adduction machine",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = emptyList(),
        description = "Sit in the machine with your back against the pad and legs outside the leg pads. Squeeze legs inward against the resistance, then slowly release back to the starting position.",
        image = R.drawable.generic_machine
    ),
    Exercise(
        name = "Machine glute kickback",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.HAMSTRINGS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Position yourself in the machine with your chest against the pad and one foot on the platform. Push the platform back by extending your hip, then return to the starting position.",
        image = R.drawable.generic_machine
    ),

    // Dumbbell
    Exercise(
        name = "Bulgarian split squat",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        description = "Stand with one foot elevated behind you on a bench. With your front foot forward, lower your body by bending your front knee, keeping your torso upright. Drive through the front heel to return to the starting position.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell lunges",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES),
        description = "Hold a dumbbell in each hand with arms at your sides. Step forward with one leg and lower your body until the front knee is bent at 90 degrees. Push back to the starting position and repeat with the other leg.",
        image = R.drawable.dumbbell_lunge
    ),
    Exercise(
        name = "Goblet squat",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.QUADRICEPS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.HAMSTRINGS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Hold a dumbbell close to your chest with both hands. Perform a squat by lowering your body, keeping your chest up and back straight. Push through your heels to return to the starting position.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell step ups",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.GLUTES,
        secondaryMuscles = listOf(Exercise.Muscle.QUADRICEPS),
        description = "Holding a dumbbell in each hand, step onto a bench or platform with one foot, pushing through the heel to bring your other foot up. Step down with the leading foot and repeat.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell Romanian deadlift",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.HAMSTRINGS,
        secondaryMuscles = listOf(Exercise.Muscle.GLUTES, Exercise.Muscle.BACK),
        image = R.drawable.generic_dumbbell,
        description = "Stand with feet hip-width apart, holding a dumbbell in each hand. With a slight bend in your knees, hinge at your hips to lower the dumbbells along your legs until you feel a stretch in your hamstrings. Return to standing by driving your hips forward.",
    ),


    /*
        Calves
     */
    // Barbell
    Exercise(
        name = "Barbell Calf raises",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        description = "Stand with a barbell on your upper back across the shoulders. Raise your heels off the ground by pushing through the balls of your feet, then slowly lower back down.",
        image = R.drawable.calf
    ),
    // Cables
    // Bodyweight
    Exercise(
        name = "Calf raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand on a raised surface or step with your heels hanging off. Push through the balls of your feet to raise your heels, then lower back down for a full stretch.",
        image = R.drawable.calf
    ),
    Exercise(
        name = "Single leg calf raises",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        description = "Stand on one foot on a raised surface with the other foot off the edge. Raise your heel as high as possible, then lower back down. Switch feet and repeat.",
        image = R.drawable.calf
    ),
    // Machine
    Exercise(
        name = "Machine calf raises",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        description = "Use a calf raise machine, sitting with the machine pads resting on your thighs. Press your toes to raise the heels, then lower back down slowly.",
        image = R.drawable.calf
    ),
    // Dumbbell
    Exercise(
        name = "Dumbbell calf raises",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.CALVES,
        secondaryMuscles = emptyList(),
        description = "Hold a dumbbell in one hand while standing on a step or raised platform. Raise your heels above the edge by pushing through the balls of your feet, then lower back down for a stretch. Switch the dumbbell to the other hand and repeat.",
        image = R.drawable.calf
    ),

    /*
        Shoulders
     */
    // Barbell
    Exercise(
        name = "Push and press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        description = "This typically refers to a push-up followed by a pressing movement, such as a dumbbell shoulder press. Perform a push-up, then stand up and execute a shoulder press.",
        image = R.drawable.shoulder_press
    ),
    Exercise(
        name = "Shoulder press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit or stand with a barbell or dumbbells at shoulder height. Press the weights overhead until your arms are fully extended, then lower them back to shoulder height.",
        image = R.drawable.shoulder_press
    ),
    Exercise(
        name = "Clean and press",
        equipment = Exercise.Equipment.BARBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.BACK, Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST, Exercise.Muscle.QUADRICEPS, Exercise.Muscle.ABS),
        image = R.drawable.clean_press,
        description = "Start with a barbell on the ground. Perform a clean by lifting the bar to your shoulders in one fluid motion. Then, press the bar overhead by extending your arms fully before lowering it back down.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    // Cables
    Exercise(
        name = "Cable side raise",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        description = "Attach a handle to a low pulley. Stand side-on to the machine, grab the handle with the hand farthest from the machine, and lift your arm to the side until it's parallel with the floor, then lower back down.",
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable rear delt fly",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        description = "Stand centered between two low pulley machines with handles attached. Grasp the left handle with your right hand and the right handle with your left hand. With arms slightly bent, pull the handles out to your sides and back, squeezing shoulder blades together, then return to the start.",
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable face pull",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        description = "Attach a rope to a high pulley. Pull the rope towards your face, separating your hands as you pull while keeping your upper arms parallel to the ground. Return slowly to the starting position.",
        image = R.drawable.generic_cable
    ),
    Exercise(
        name = "Cable shoulder press",
        equipment = Exercise.Equipment.CABLES,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        description = "Attach a handle to a low pulley and sit directly in front of it. Hold the handle with one hand and press it overhead. Repeat with the other hand or use a setup with two handles for both hands simultaneously.",
        image = R.drawable.generic_cable
    ),
    // Bodyweight
    Exercise(
        name = "Handstand push ups",
        equipment = Exercise.Equipment.BODY_WEIGHT,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        image = R.drawable.headstand_push_up,
        description = "Get into a handstand position against a wall for support. Lower your body by bending your elbows until your head nearly touches the floor, then push back up to the starting position.",
        difficulty = Exercise.ExerciseDifficulty.ADVANCED
    ),
    // Machine
    Exercise(
        name = "Machine shoulder press",
        equipment = Exercise.Equipment.MACHINE,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit in a shoulder press machine with your back against the pad. Grip the handles and press them overhead until your arms are extended, then lower back to the start.",
        image = R.drawable.generic_machine
    ),
    // Dumbbell
    Exercise(
        name = "Front raise",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand with dumbbells at your sides. Lift the weights straight in front of you to shoulder height with a slight bend in your elbows, then lower them back down.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Side raise",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Stand holding dumbbells at your sides. Lift the weights out to the sides until they are parallel with the floor, then lower them back down.",
        image = R.drawable.generic_dumbbell
    ),
    Exercise(
        name = "Dumbbell shoulder press",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = listOf(Exercise.Muscle.TRICEPS, Exercise.Muscle.CHEST),
        image = R.drawable.dumbbell_shoulder_press,
        difficulty = Exercise.ExerciseDifficulty.BEGINNER,
        description = "Sit or stand holding dumbbells at shoulder height. Press the weights overhead until your arms are fully extended, then lower them back to the starting position.",
        variations = listOf(
            "Arnold press"
        )
    ),
    Exercise(
        name = "Dumbbell rear delt row",
        equipment = Exercise.Equipment.DUMBBELL,
        primaryMuscle = Exercise.Muscle.SHOULDERS,
        secondaryMuscles = emptyList(),
        description = "Bend forward at the hips with a dumbbell in each hand, arms hanging straight down. Row the dumbbells towards your hips, keeping elbows out and squeezing your shoulder blades together, then lower back down.",
        image = R.drawable.generic_dumbbell
    ),

    // Other
    Exercise(
        name = "Can you hear the silence?",
        equipment = Exercise.Equipment.EVERYTHING,
        primaryMuscle = Exercise.Muscle.EVERYTHING,
        secondaryMuscles = emptyList(),
        image = R.drawable.gigachad
    ),
)
