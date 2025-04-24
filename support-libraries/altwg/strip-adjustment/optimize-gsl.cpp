#include <gsl/gsl_multimin.h>


/*-----------------------------------------------------------------------
-------------------------------------------------------------------------
 This file contains GSL solver related functions
-------------------------------------------------------------------------
-----------------------------------------------------------------------*/


#define DX 0.00001


struct FuncParam
{
    double (*function)(const double*, void* externalParams);
    void (*gradient)(const double*, double*, void *externalParams);
    void* externalParams;
};


#ifndef NDEBUG
static void printCurrentValue(size_t iter, double fx, const gsl_vector* x, size_t N)
{
    printf("Iter: %ld,  fx = %.12f", iter, fx);
    /*size_t i;
    for (i=0; i<N; ++i)
    {
        printf(", x[%ld] = %f", i, gsl_vector_get(x, i));
    }*/
    printf("\n");
}
#endif

static double func(const gsl_vector *v, void *internalParams)
{
    struct FuncParam* p = (struct FuncParam*)internalParams;
    return p->function(gsl_vector_const_ptr(v, 0), p->externalParams);
}


/************************************************************************
* This function numerically computes the gradient of func using finite differences
************************************************************************/
static void grad(const gsl_vector *v,
                 void *internalParams,
                 gsl_vector *df)
{
    struct FuncParam* p = (struct FuncParam*)internalParams;
    if (p->gradient != 0)
    {
        p->gradient(gsl_vector_const_ptr(v, 0), gsl_vector_ptr(df, 0), p->externalParams);
        return;
    }

    double f = func(v, internalParams);
    size_t N = v->size;

    size_t i;
    size_t j;
    for (i=0; i<N; ++i)
    {
        double coef2[N];
        for (j=0; j<N; ++j)
            coef2[j] = gsl_vector_get(v,j);

        coef2[i] += DX;

        double f2 = p->function(coef2, p->externalParams);

        gsl_vector_set(df,i, (f2 - f) / DX);
    }
}


static void fdf(const gsl_vector *x,
                void *params,
                double *f,
                gsl_vector *df)
{
    *f = func(x, params);
    grad(x, params, df);
}


/*
  Public function

  Perform an optimization on provided function with N independent variables.
  minimizer contains the initial guess and on output contains the optimal
  values of the independent function that minimizes the function.
 */
void optimizeGsl(double (*function)(const double*, void *externalParams),
                 void (*gradient)(const double*, double*, void *externalParams),
                 double* minimizer,
                 size_t N,
                 void *externalParams)
{
    struct FuncParam internalParams;
    internalParams.function = function;
    internalParams.gradient = gradient;
    internalParams.externalParams = externalParams;

    size_t iter = 0;
    int status;

    const gsl_multimin_fdfminimizer_type *T;
    gsl_multimin_fdfminimizer *s;

    gsl_vector *x;
    gsl_multimin_function_fdf my_func;

    my_func.n = N;
    my_func.f = func;
    my_func.df = grad;
    my_func.fdf = fdf;
    my_func.params = &internalParams;

    /* Starting point, x =  */
    x = gsl_vector_alloc (N);
    size_t i;
    for (i=0;i<N;++i)
        gsl_vector_set (x, i, minimizer[i]);

    T = gsl_multimin_fdfminimizer_conjugate_pr;
    /*T = gsl_multimin_fdfminimizer_vector_bfgs2;*/
    s = gsl_multimin_fdfminimizer_alloc (T, N);

    gsl_multimin_fdfminimizer_set (s, &my_func, x, 0.5, 1e-1);

#ifndef NDEBUG
    printf("Initial value of objective function: \n");
    printCurrentValue(0, function(minimizer, externalParams), x, N);
    printf("\n");
#endif

    do
    {
        iter++;
        status = gsl_multimin_fdfminimizer_iterate (s);

        if (status)
            break;

        status = gsl_multimin_test_gradient (s->gradient, 1e-1);

#ifndef NDEBUG
        if (status == GSL_SUCCESS)
            printf ("Minimum found at:\n");

        printCurrentValue(iter, s->f, s->x, N);
#endif
    }
    while (status == GSL_CONTINUE && iter < 50);

    for (i=0;i<N;++i)
        minimizer[i] = gsl_vector_get (s->x, i);

#ifndef NDEBUG
    printf("GSL optimization terminated with status code = %d\n", status);
    printCurrentValue(iter, function(minimizer, externalParams), s->x, N);
    printf("\n");
#endif

    gsl_multimin_fdfminimizer_free (s);
    gsl_vector_free (x);
}
