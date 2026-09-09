'use client';
import { Controller, Control, FieldValues, Path } from 'react-hook-form';

type Props<T extends FieldValues> = {
  control: Control<T>;
  name: Path<T>;
  label: string;
  placeholder: string;
  type?: string;
  autoComplete?: string;
  touched?: boolean;
};

export default function InputField<T extends FieldValues>({
  control,
  name,
  label,
  placeholder,
  type = 'text',
  autoComplete,
}: Props<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <div>
          <label htmlFor={name} className='sr-only'>
            {label}
          </label>
          <input
            id={name}
            className='w-full rounded-3xl border p-2 dark:text-white focus:outline-none focus:ring-2'
            type={type}
            placeholder={placeholder}
            autoComplete={autoComplete}
            {...field}
          />
          <p className='text-amber-600 min-h-6'>{fieldState.error?.message}</p>
        </div>
      )}
    />
  );
}
