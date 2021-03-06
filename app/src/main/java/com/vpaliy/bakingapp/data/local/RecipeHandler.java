package com.vpaliy.bakingapp.data.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.vpaliy.bakingapp.data.model.IngredientEntity;
import com.vpaliy.bakingapp.data.model.RecipeEntity;
import com.vpaliy.bakingapp.data.model.StepEntity;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

import static com.vpaliy.bakingapp.data.local.RecipeContract.Recipes;
import static com.vpaliy.bakingapp.data.local.RecipeContract.Ingredients;
import static com.vpaliy.bakingapp.data.local.RecipeContract.Steps;
import static com.vpaliy.bakingapp.data.local.RecipeDatabaseHelper.RecipesIngredients;

@Singleton
public class RecipeHandler {

    private ContentResolver contentResolver;

    private static final String TAG=RecipeHandler.class.getSimpleName();

    private RecipeHandler(ContentResolver resolver){
        this.contentResolver=resolver;
    }

    public static RecipeHandler start(ContentResolver resolver){
        return new RecipeHandler(resolver);
    }

    public RecipeHandler insert(RecipeEntity recipeEntity){
        if(recipeEntity!=null){
            ContentValues values=DatabaseUtils.toValues(recipeEntity);
            contentResolver.insert(Recipes.CONTENT_URI,values);
        }
        return this;
    }

    public RecipeHandler insertIngredients(int recipeId, List<IngredientEntity> ingredients){
        if(ingredients!=null) {
            ContentValues values = new ContentValues();
            ingredients.forEach(ingredient -> {
                values.clear();
                values.put(RecipesIngredients.RECIPE_ID, recipeId);
                values.put(RecipesIngredients.INGREDIENT_ID, ingredient.getId());
                Uri contentUri = Recipes.buildRecipeWithIngredientsUri(Integer.toString(recipeId));
                contentResolver.insert(contentUri, values);
                contentUri=Ingredients.buildIngredientWithRecipesUri(Integer.toString(ingredient.getId()));
                contentResolver.insert(contentUri,values);
                contentResolver.insert(Ingredients.CONTENT_URI, DatabaseUtils.toValues(ingredient));
            });
        }
        return this;
    }

    public RecipeEntity queryById(int recipeId){
        Uri uri=Recipes.buildRecipeUri(Integer.toString(recipeId));
        Cursor cursor=contentResolver.query(uri,null,null,null,null);
        RecipeEntity entity=DatabaseUtils.toRecipe(cursor);
        if(cursor!=null){
            if(!cursor.isClosed()) cursor.close();
        }
        return entity;
    }

    public List<RecipeEntity> queryAll(){
        Cursor cursor=contentResolver.query(Recipes.CONTENT_URI,null,null,null,null);
        if(cursor!=null){
            Log.d(TAG,Integer.toString(cursor.getCount()));
            List<RecipeEntity> recipes=new ArrayList<>(cursor.getCount());
            while(cursor.moveToNext()){
                RecipeEntity recipeEntity=DatabaseUtils.toRecipe(cursor);
                queryStepsFor(recipeEntity).queryIngredientsFor(recipeEntity);
                recipes.add(recipeEntity);
            }
            return recipes;
        }
        return null;
    }

    public RecipeHandler queryStepsFor(RecipeEntity entity){
        if(entity!=null){
            Uri contentUri=Recipes.buildRecipeWithStepsUri(Integer.toString(entity.getId()));
            Cursor cursor=contentResolver.query(contentUri,null,null,null,null);
            if(cursor!=null){
                List<StepEntity> steps=new ArrayList<>(cursor.getCount());
                while(cursor.moveToNext()){
                    steps.add(DatabaseUtils.toStep(cursor));
                }
                entity.setSteps(steps);
                if(!cursor.isClosed()) cursor.close();
            }
        }
        return this;
    }

    public RecipeHandler queryIngredientsFor(RecipeEntity recipe){
        if(recipe!=null){
            Uri contentUri=Recipes.buildRecipeWithIngredientsUri(Integer.toString(recipe.getId()));
            Cursor cursor=contentResolver.query(contentUri,null,null,null,null);
            if(cursor!=null){
                List<IngredientEntity> ingredients=new ArrayList<>(cursor.getCount());
                while(cursor.moveToNext()){
                    ingredients.add(queryIngredient(cursor.getInt(cursor.getColumnIndex(RecipesIngredients.INGREDIENT_ID))));
                }
                if(!cursor.isClosed()) cursor.close();
                recipe.setIngredients(ingredients);
            }
        }
        return this;
    }

    private IngredientEntity queryIngredient(int id){
        Uri contentUri=Ingredients.buildIngredientUri(Integer.toString(id));
        Cursor cursor=contentResolver.query(contentUri,null,null,null,null);
        if(cursor!=null){
            IngredientEntity entity=DatabaseUtils.toIngredient(cursor);
            if(!cursor.isClosed()) cursor.close();
            return entity;
        }
        return null;
    }

    public RecipeHandler insertSteps(int recipeId, List<StepEntity> steps){
        if (steps != null) {
            steps.forEach(stepEntity -> {
                ContentValues values=DatabaseUtils.toValues(stepEntity,recipeId);
                contentResolver.insert(Steps.CONTENT_URI,values);
                values.clear();
            });
        }
        return this;
    }

}
