# Implementation Plan - Signup UI Implementation

This plan outlines the steps to implement the Signup UI for the Student Club Connect application, ensuring visual consistency with the existing Login UI while adding required student identification fields.

## User Review Required

> [!IMPORTANT]
> A **Student ID** field will be added to the Signup form as required by the application logic, even though it is not present in the provided reference image. It will be positioned between "Full Name" and "Email".

## Proposed Changes

### [Authentication UI]

#### [NEW] [activity_signup.xml](file:///D:/StudentClubConnect/app/src/main/res/layout/activity_signup.xml)
- Create a scrollable layout using `NestedScrollView`.
- Implement a `ConstraintLayout` container with 24dp padding.
- Include the application logo (`@drawable/scc_logo`) and app name (`Student Club Connect`).
- Implement the following input fields (Label + Styled EditText):
    - Full Name (inputType: `textPersonName`)
    - Student ID (inputType: `text`)
    - Email (inputType: `textEmailAddress`)
    - Password (inputType: `textPassword`)
    - Confirm Password (inputType: `textPassword`)
- Reuse existing styles: `@style/Typography.Heading`, `@style/Typography.Label`, `@style/Component.Input`, and `@style/Component.Button.Primary`.
- Use `TextInputLayout` wrappers to support better error messaging while maintaining the visual style of the Login screen.

#### [NEW] [SignupActivity.kt](file:///D:/StudentClubConnect/app/src/main/java/com/studentclubconnect/ui/auth/SignupActivity.kt)
- Implement `ViewBinding` for UI interaction.
- Implement comprehensive field validation:
    - Non-empty checks for all fields.
    - Email format validation using `Patterns.EMAIL_ADDRESS`.
    - Password length check (min 6 characters).
    - Password matching check.
- Display validation errors using `TextInputLayout.error`.
- Implement navigation to `LoginActivity` for the "Login" text action.
- Show a "Signup details are valid" Toast upon successful local validation.

#### [MODIFY] [LoginActivity.kt](file:///D:/StudentClubConnect/app/src/main/java/com/studentclubconnect/LoginActivity.kt)
- Update the `btnCreateAccount` click listener to navigate to `SignupActivity`.

#### [MODIFY] [AndroidManifest.xml](file:///D:/StudentClubConnect/app/src/main/AndroidManifest.xml)
- Register `SignupActivity` in the manifest.

## Verification Plan

### Manual Verification
- **Navigation:** Verify that tapping "Create Account" on the Login screen opens the Signup screen, and tapping "Login" on the Signup screen returns to the Login screen.
- **Validation:**
    - Test empty fields to trigger "Please enter..." errors.
    - Test invalid email format.
    - Test password shorter than 6 characters.
    - Test mismatched passwords.
- **Visuals:** Compare the Signup screen with the Login screen and reference image to ensure consistent colors, fonts, and spacing.
- **Responsiveness:** Open the keyboard on each input field to ensure the layout scrolls correctly and no components are clipped.
- **Build:** Run `./gradlew assembleDebug` to ensure no compilation errors.
