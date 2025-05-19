import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { StepDetailComponent } from './step-detail.component';

describe('Step Management Detail Component', () => {
  let comp: StepDetailComponent;
  let fixture: ComponentFixture<StepDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./step-detail.component').then(m => m.StepDetailComponent),
              resolve: { step: () => of({ id: 20214 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(StepDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StepDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load step on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', StepDetailComponent);

      // THEN
      expect(instance.step()).toEqual(expect.objectContaining({ id: 20214 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
